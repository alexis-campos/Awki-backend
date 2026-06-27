package com.awki.chat.port;

import java.util.UUID;

public interface RiesgoChatPort {
    void analizarRiesgo(UUID embarazoId, String contenido, boolean alarmaProbable);

    // Implementation stub for Module 6 Integration
    @org.springframework.stereotype.Component
    class StubRiesgoChatPort implements RiesgoChatPort {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StubRiesgoChatPort.class);

        @Override
        public void analizarRiesgo(UUID embarazoId, String contenido, boolean alarmaProbable) {
            log.info("[RiesgoChatPort STUB] Analizando riesgo para embarazo: {}. Alarma probable detectada: {}", embarazoId, alarmaProbable);
            // TODO: Integrar con el Motor de Riesgo real cuando el Módulo 6 esté desarrollado
        }
    }
}
