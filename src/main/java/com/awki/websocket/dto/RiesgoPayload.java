package com.awki.websocket.dto;

import java.util.List;
import java.util.UUID;

public record RiesgoPayload(
        UUID embarazoId,
        UUID pacienteId,
        String nombrePaciente,
        String nivelRiesgo,
        List<String> factores,
        long calculadoEn
) {}
