package com.awki.epicrisis.service;

import com.awki.alerta.service.AlertaService;
import com.awki.auth.entity.Paciente;
import com.awki.auth.service.AuthService;
import com.awki.chat.service.ChatService;
import com.awki.embarazo.entity.AntecedentesClinicos;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.service.EmbarazoService;
import com.awki.epicrisis.entity.Epicrisis;
import com.awki.epicrisis.entity.EpicrisisJob;
import com.awki.epicrisis.entity.EstadoJob;
import com.awki.epicrisis.repository.EpicrisisJobRepository;
import com.awki.epicrisis.repository.EpicrisisRepository;
import com.awki.epicrisis.util.PdfGeneratorHelper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpicrisisAsyncService {

    private final EpicrisisJobRepository jobRepository;
    private final EpicrisisRepository epicrisisRepository;
    private final AuthService authService;
    private final EmbarazoService embarazoService;
    private final AlertaService alertaService;
    private final ChatService chatService;
    private final DocumentStorageService storageService;
    private final ObjectMapper objectMapper;

    @Async("taskExecutor")
    @Transactional
    public void generarEpicrisisAsync(
            UUID jobId,
            UUID embarazoId,
            UUID medicoId,
            UUID clinicaId,
            String motivoDerivacion,
            String observacionesAdicionales
    ) {
        log.info("Iniciando procesamiento asíncrono para EpicrisisJob: {}", jobId);

        EpicrisisJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("EpicrisisJob no encontrado: {}", jobId);
            return;
        }

        try {
            Embarazo embarazo = embarazoService.getEmbarazoEntityById(embarazoId);

            Paciente paciente = authService.getPacienteEntityById(embarazo.getPacienteId());

            String pacienteNombre = paciente.getNombres() + " " + paciente.getApellidos();
            long semanas = ChronoUnit.WEEKS.between(embarazo.getFechaUltimaMenstruacion(), LocalDate.now());
            String semanasGestacion = semanas + " semanas (FUM: " + embarazo.getFechaUltimaMenstruacion() + ")";

            String antecedentesStr = obtenerAntecedentesFormateados(embarazo.getAntecedentes());

            String resumenChat = chatService.getContenidoResumen(embarazoId)
                    .orElse("No se registran interacciones previas en el chat de seguimiento.");

            String alertasStr = alertaService.getAlertasFormatadasParaEpicrisis(embarazoId);

            String prompt = construirPrompt(pacienteNombre, semanasGestacion, motivoDerivacion, observacionesAdicionales, antecedentesStr, resumenChat, alertasStr);

            // GeminiClient es accedido a través de ChatService para no violar límites de módulo
            // TODO: extraer LlmClient como abstracción común cuando se desacople el módulo chat
            String resumenIa = null;
            String sintesisClinica = null;
            String conclusiones = null;
            boolean generadoSinIa = false;

            var geminiRespuestaOpt = chatService.generarContenidoConGemini(prompt, 0.3, 2000);

            if (geminiRespuestaOpt.isPresent()) {
                String rawText = geminiRespuestaOpt.get().trim();
                if (rawText.startsWith("```")) {
                    rawText = rawText.replaceAll("(?s)^```(?:json)?|```$", "").trim();
                }
                try {
                    GeminiEpicrisisJson parsed = objectMapper.readValue(rawText, GeminiEpicrisisJson.class);
                    resumenIa = parsed.resumen_ia();
                    sintesisClinica = parsed.sintesis_clinica();
                    conclusiones = parsed.conclusiones();
                } catch (Exception ex) {
                    log.warn("Gemini retornó un JSON inválido, activando fallback local: {}", ex.getMessage());
                }
            }

            if (resumenIa == null) {
                generadoSinIa = true;
                resumenIa = "Generación de IA no disponible temporalmente. Se consolidó este informe con los datos clínicos almacenados en el sistema.";
                sintesisClinica = String.format("Paciente gestante con %d semanas de embarazo. Antecedentes clínicos activos: %s. Historial del chat de IA: %s.",
                        semanas, antecedentesStr, resumenChat);
                conclusiones = String.format("Ingreso a labor de parto. Motivo de derivación: %s. Alertas clínicas reportadas previamente: %s.",
                        motivoDerivacion, alertasStr);
            }

            GeminiEpicrisisJson finalJson = new GeminiEpicrisisJson(resumenIa, sintesisClinica, conclusiones, generadoSinIa);
            String contenidoJsonFinal = objectMapper.writeValueAsString(finalJson);

            byte[] pdfBytes = PdfGeneratorHelper.generarPdfEpicrisis(
                    pacienteNombre,
                    semanasGestacion,
                    motivoDerivacion,
                    observacionesAdicionales,
                    resumenIa,
                    sintesisClinica,
                    conclusiones
            );

            String uniqueFileName = "epicrisis_" + embarazoId + "_" + UUID.randomUUID() + ".pdf";
            String savedFileName = storageService.guardarDocumento(pdfBytes, uniqueFileName);

            Epicrisis epicrisis = new Epicrisis();
            epicrisis.setEmbarazoId(embarazoId);
            epicrisis.setMedicoId(medicoId);
            epicrisis.setClinicaId(clinicaId);
            epicrisis.setMotivoDerivacion(motivoDerivacion);
            epicrisis.setObservacionesAdicionales(observacionesAdicionales);
            epicrisis.setContenidoJson(contenidoJsonFinal);
            epicrisis.setUrlPdf(savedFileName);
            Epicrisis guardada = epicrisisRepository.save(epicrisis);

            job.setEstado(EstadoJob.COMPLETADO);
            job.setEpicrisis(guardada);
            jobRepository.save(job);
            log.info("EpicrisisJob procesado con éxito. Job ID: {} - Epicrisis ID: {}", jobId, guardada.getId());

        } catch (Exception e) {
            log.error("Error procesando EpicrisisJob {}: {}", jobId, e.getMessage(), e);
            job.setEstado(EstadoJob.FALLIDO);
            job.setMensajeError(e.getMessage());
            jobRepository.save(job);
        }
    }

    private String obtenerAntecedentesFormateados(AntecedentesClinicos ant) {
        if (ant == null) return "Sin antecedentes registrados.";
        StringBuilder sb = new StringBuilder();
        if (ant.isDiabetesPrevia()) sb.append("Diabetes previa, ");
        if (ant.isHipertensionPrevia()) sb.append("Hipertensión previa, ");
        if (ant.isPreeclampsiaPrevia()) sb.append("Preeclampsia previa, ");
        if (ant.isEnfermedadRenal()) sb.append("Enfermedad renal, ");
        if (ant.isEnfermedadAutoinmune()) sb.append("Enfermedad autoinmune, ");
        if (ant.isAnemiaPrevia()) sb.append("Anemia previa, ");
        if (ant.isVihPositivo()) sb.append("VIH+, ");
        if (ant.isSifilisPrevia()) sb.append("Sífilis previa, ");
        if (ant.isTrastornoCoagulacion()) sb.append("Trastorno de coagulación, ");
        if (ant.isEdadMaternaRiesgo()) sb.append("Edad materna de riesgo, ");
        if (ant.isResidenciaAltitud()) sb.append("Residencia en altitud, ");

        if (sb.length() == 0) return "Ninguno de importancia.";
        return sb.substring(0, sb.length() - 2);
    }

    private String construirPrompt(
            String pacienteNombre, String semanas, String motivo,
            String observaciones, String antecedentes, String resumenChat, String alertas
    ) {
        return "Eres un asistente médico experto. Genera un reporte de derivación o Epicrisis obstétrica estructurada en formato JSON para una paciente gestante que ingresa a sala de partos.\n" +
                "Los datos clínicos consolidados de la paciente son los siguientes:\n" +
                "- Nombre de la Paciente: " + pacienteNombre + "\n" +
                "- Semanas de Gestación: " + semanas + "\n" +
                "- Motivo de Derivación: " + motivo + "\n" +
                "- Observaciones Adicionales: " + (observaciones != null ? observaciones : "Ninguna") + "\n" +
                "- Antecedentes Clínicos: " + antecedentes + "\n" +
                "- Resumen del Chat de Seguimiento (Módulo 5): " + resumenChat + "\n" +
                "- Alertas Clínicas Recientes Detectadas: " + alertas + "\n\n" +
                "IMPORTANTE:\n" +
                "- NO incluyas mensajes literales de chat de la paciente en el reporte para proteger su privacidad.\n" +
                "- La respuesta DEBE ser un objeto JSON válido, sin formato adicional (sin markdown, sin bloques ```json).\n" +
                "El JSON debe contener exactamente estos campos:\n" +
                "{\n" +
                "  \"resumen_ia\": \"(Síntesis de derivación basada en las semanas, controles y motivo de derivación)\",\n" +
                "  \"sintesis_clinica\": \"(Resumen de la evolución durante la gestación basado en el historial del chat y antecedentes)\",\n" +
                "  \"conclusiones\": \"(Recomendaciones médicas de derivación final indicando si presenta factores de riesgo activos)\"\n" +
                "}";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiEpicrisisJson(
            String resumen_ia,
            String sintesis_clinica,
            String conclusiones,
            Boolean generado_sin_ia
    ) {}
}
