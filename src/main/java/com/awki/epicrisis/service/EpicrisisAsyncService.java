package com.awki.epicrisis.service;

import com.awki.alerta.entity.Alerta;
import com.awki.alerta.repository.AlertaRepository;
import com.awki.auth.entity.Paciente;
import com.awki.auth.repository.PacienteRepository;
import com.awki.chat.entity.ResumenClinico;
import com.awki.chat.repository.ResumenClinicoRepository;
import com.awki.chat.service.GeminiClient;
import com.awki.embarazo.entity.AntecedentesClinicos;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.repository.EmbarazoRepository;
import com.awki.epicrisis.entity.Epicrisis;
import com.awki.epicrisis.entity.EpicrisisJob;
import com.awki.epicrisis.entity.EstadoJob;
import com.awki.epicrisis.repository.EpicrisisJobRepository;
import com.awki.epicrisis.repository.EpicrisisRepository;
import com.awki.epicrisis.util.PdfGeneratorHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpicrisisAsyncService {

    private final EpicrisisJobRepository jobRepository;
    private final EpicrisisRepository epicrisisRepository;
    private final EmbarazoRepository embarazoRepository;
    private final PacienteRepository pacienteRepository;
    private final AlertaRepository alertaRepository;
    private final ResumenClinicoRepository resumenClinicoRepository;
    private final GeminiClient geminiClient;
    private final DocumentStorageService storageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
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
            // 1. Obtener datos clínicos consolidados
            Embarazo embarazo = embarazoRepository.findById(embarazoId)
                    .orElseThrow(() -> new IllegalArgumentException("Embarazo no encontrado"));

            Paciente paciente = pacienteRepository.findById(embarazo.getPacienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

            String pacienteNombre = paciente.getNombres() + " " + paciente.getApellidos();
            long semanas = ChronoUnit.WEEKS.between(embarazo.getFechaUltimaMenstruacion(), LocalDate.now());
            String semanasGestacion = semanas + " semanas (FUM: " + embarazo.getFechaUltimaMenstruacion() + ")";

            // Antecedentes
            String antecedentesStr = obtenerAntecedentesFormateados(embarazo.getAntecedentes());

            // Resumen de chat
            Optional<ResumenClinico> resumenOpt = resumenClinicoRepository.findById(embarazoId);
            String resumenChat = resumenOpt.map(ResumenClinico::getContenidoResumen)
                    .orElse("No se registran interacciones previas en el chat de seguimiento.");

            // Alertas
            List<Alerta> alertas = alertaRepository.findByEmbarazo_Id(embarazoId);
            String alertasStr = alertas.isEmpty() ? "Sin alertas clínicas registradas." :
                    alertas.stream()
                            .map(a -> "- " + a.getTipoAlerta() + " (" + a.getNivelUrgencia() + "): " + a.getDescripcion())
                            .collect(Collectors.joining("\n"));

            // 2. Intentar llamar a Gemini API con fallback
            String prompt = construirPrompt(pacienteNombre, semanasGestacion, motivoDerivacion, observacionesAdicionales, antecedentesStr, resumenChat, alertasStr);
            
            // TODO: En un refactor posterior, desacoplar GeminiClient en una abstracción LlmClient común
            Optional<String> respuestaOpt = geminiClient.generarContenido(prompt, 0.3, 2000);
            
            String jsonReport = null;
            String resumenIa = null;
            String sintesisClinica = null;
            String conclusiones = null;
            boolean generadoSinIa = false;

            if (respuestaOpt.isPresent()) {
                String rawText = respuestaOpt.get().trim();
                // Limpiar posibles bloques markdown ```json
                if (rawText.startsWith("```")) {
                    rawText = rawText.replaceAll("(?s)^```(?:json)?|```$", "").trim();
                }
                try {
                    GeminiEpicrisisJson parsed = objectMapper.readValue(rawText, GeminiEpicrisisJson.class);
                    resumenIa = parsed.resumen_ia();
                    sintesisClinica = parsed.sintesis_clinica();
                    conclusiones = parsed.conclusiones();
                    jsonReport = rawText;
                } catch (Exception ex) {
                    log.warn("Gemini retornó un JSON inválido, activando fallback local: {}", ex.getMessage());
                }
            }

            // Activar Fallback local si la llamada a Gemini falló o el JSON fue inválido
            if (resumenIa == null) {
                generadoSinIa = true;
                resumenIa = "Generación de IA no disponible temporalmente. Se consolidó este informe con los datos clínicos almacenados en el sistema.";
                sintesisClinica = String.format("Paciente gestante con %d semanas de embarazo. Antecedentes clínicos activos: %s. Historial del chat de IA: %s.",
                        semanas, antecedentesStr, resumenChat);
                conclusiones = String.format("Ingreso a labor de parto. Motivo de derivación: %s. Alertas clínicas reportadas previamente: %s.",
                        motivoDerivacion, alertasStr);
                
                GeminiEpicrisisJson fallbackJson = new GeminiEpicrisisJson(resumenIa, sintesisClinica, conclusiones);
                jsonReport = objectMapper.writeValueAsString(fallbackJson);
            }

            // 3. Agregar flag de generación sin IA al contenido JSON guardado en base de datos
            String contenidoJsonFinal = jsonReport;
            if (generadoSinIa) {
                contenidoJsonFinal = contenidoJsonFinal.substring(0, contenidoJsonFinal.length() - 1) + ", \"generado_sin_ia\": true}";
            } else {
                contenidoJsonFinal = contenidoJsonFinal.substring(0, contenidoJsonFinal.length() - 1) + ", \"generado_sin_ia\": false}";
            }

            // 4. Generar el documento PDF físico
            byte[] pdfBytes = PdfGeneratorHelper.generarPdfEpicrisis(
                    pacienteNombre,
                    semanasGestacion,
                    motivoDerivacion,
                    observacionesAdicionales,
                    resumenIa,
                    sintesisClinica,
                    conclusiones
            );

            // 5. Almacenar el PDF en el proveedor
            String uniqueFileName = "epicrisis_" + embarazoId + "_" + UUID.randomUUID() + ".pdf";
            String savedFileName = storageService.guardarDocumento(pdfBytes, uniqueFileName);

            // 6. Persistir el registro final en Epicrisis
            Epicrisis epicrisis = new Epicrisis();
            epicrisis.setEmbarazoId(embarazoId);
            epicrisis.setMedicoId(medicoId);
            epicrisis.setClinicaId(clinicaId);
            epicrisis.setMotivoDerivacion(motivoDerivacion);
            epicrisis.setObservacionesAdicionales(observacionesAdicionales);
            epicrisis.setContenidoJson(contenidoJsonFinal);
            epicrisis.setUrlPdf(savedFileName); // Guardamos el nombre del archivo para resolución interna
            Epicrisis guardada = epicrisisRepository.save(epicrisis);

            // 7. Completar el Job
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

    // Record interno para mapear y deserializar la respuesta del LLM
    private record GeminiEpicrisisJson(String resumen_ia, String sintesis_clinica, String conclusiones) {}
}
