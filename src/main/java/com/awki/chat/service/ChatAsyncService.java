package com.awki.chat.service;

import com.awki.chat.entity.MensajeChat;
import com.awki.chat.entity.ResumenClinico;
import com.awki.chat.port.AlertaChatPort;
import com.awki.chat.port.RiesgoChatPort;
import com.awki.chat.repository.ResumenClinicoRepository;
import com.awki.embarazo.entity.Embarazo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ChatAsyncService.class);

    private final RiesgoChatPort riesgoChatPort;
    private final AlertaChatPort alertaChatPort;

    @Async("taskExecutor")
    public void analizarRiesgoAsync(UUID embarazoId, String contenido, boolean alarmaProbable) {
        log.info("[ChatAsync] Iniciando análisis de riesgo asíncrono para embarazo: {}", embarazoId);
        riesgoChatPort.analizarRiesgo(embarazoId, contenido, alarmaProbable);
    }

    @Async("taskExecutor")
    public void crearAlertaSiCorrespondeAsync(UUID embarazoId, String contenido, boolean alarmaProbable) {
        log.info("[ChatAsync] Iniciando evaluación de alertas asíncronas para embarazo: {}", embarazoId);
        alertaChatPort.crearAlertaSiCorresponde(embarazoId, contenido, alarmaProbable);
    }

    @Async("taskExecutor")
    public void regenerarResumenAsync(
            UUID embarazoId, 
            List<MensajeChat> mensajes, 
            ResumenClinicoRepository resumenRepository, 
            Embarazo embarazo, 
            GeminiClient geminiClient
    ) {
        log.info("[ChatAsync] Regenerando resumen clínico asíncronamente para embarazo: {}", embarazoId);
        
        if (mensajes.isEmpty()) {
            log.info("[ChatAsync] No hay mensajes de chat para generar un resumen para el embarazo: {}", embarazoId);
            return;
        }

        // Construir la transcripción resumida para el prompt
        String transcripcion = mensajes.stream()
                .map(m -> m.getRol().name() + ": " + m.getContenido())
                .collect(Collectors.joining("\n"));

        String prompt = "Genera un resumen clínico breve para un médico a partir de estos mensajes de una gestante. No transcribas la conversación completa. Extrae solo datos clínicamente relevantes: síntomas mencionados, frecuencia, intensidad si aparece, posibles signos de alarma, dudas recurrentes, estado emocional relevante y recomendaciones de seguimiento. No incluyas información íntima no clínica. No inventes datos. Escribe en español formal y claro.\n\nConversación:\n" + transcripcion;

        String contenidoResumen;
        String modeloGenerado = "Gemini API";

        try {
            var responseOpt = geminiClient.generarContenido(prompt);
            if (responseOpt.isPresent()) {
                contenidoResumen = responseOpt.get();
            } else {
                log.warn("[ChatAsync] La API de Gemini no devolvió respuesta, usando resumen local de contingencia.");
                contenidoResumen = generarResumenLocalBasico(mensajes);
                modeloGenerado = "Fallback Local";
            }
        } catch (Exception e) {
            log.error("[ChatAsync] Error llamando a Gemini para regenerar resumen: {}. Usando fallback local.", e.getMessage());
            contenidoResumen = generarResumenLocalBasico(mensajes);
            modeloGenerado = "Fallback Local";
        }

        // Guardar o actualizar en base de datos
        try {
            ResumenClinico resumen = resumenRepository.findById(embarazoId)
                    .orElseGet(() -> {
                        ResumenClinico r = new ResumenClinico();
                        r.setEmbarazoId(embarazoId);
                        r.setEmbarazo(embarazo);
                        r.setNewEntity(true);
                        return r;
                    });

            resumen.setContenidoResumen(contenidoResumen);
            resumen.setGeneradoPorModelo(modeloGenerado);
            resumen.setUpdatedAt(LocalDateTime.now());
            resumenRepository.save(resumen);

            log.info("[ChatAsync] Resumen clínico guardado con éxito para embarazo: {}", embarazoId);
        } catch (Exception e) {
            log.error("[ChatAsync] Error al persistir el resumen clínico en PostgreSQL: {}", e.getMessage());
        }
    }

    private String generarResumenLocalBasico(List<MensajeChat> mensajes) {
        long mensajesPaciente = mensajes.stream().filter(m -> "PACIENTE".equals(m.getRol().name())).count();
        long mensajesConAlarma = mensajes.stream().filter(MensajeChat::isAlarmaProbable).count();

        return "Resumen Clínico Básico (Generado Localmente):\n"
                + "- Se registraron " + mensajesPaciente + " mensajes de la gestante.\n"
                + "- Se detectaron " + mensajesConAlarma + " posibles signos de alarma o alerta.\n"
                + "- Revisar el estado de la paciente en consulta presencial.";
    }
}
