package com.awki.chat.port;

import java.util.UUID;

public interface AlertaChatPort {
    void crearAlertaSiCorresponde(UUID embarazoId, String contenido, boolean alarmaProbable);

    // Implementation stub for Module 7 Integration
    @org.springframework.stereotype.Component
    class StubAlertaChatPort implements AlertaChatPort {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StubAlertaChatPort.class);

        @Override
        public void crearAlertaSiCorresponde(UUID embarazoId, String contenido, boolean alarmaProbable) {
            log.info("[AlertaChatPort STUB] Procesando alerta para embarazo: {}. Alarma probable: {}", embarazoId, alarmaProbable);
            if (alarmaProbable) {
                log.warn("[AlertaChatPort STUB] ALERTA CRÍTICA GENERADA para embarazo: {} - Contenido: '{}'", embarazoId, contenido);
                // TODO: Integrar con el Módulo 7 (Alertas y Notificaciones) enviando WebSocket/SMS
            }
        }
    }
}
