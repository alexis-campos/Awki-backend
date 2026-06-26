package com.awki.websocket.dto;

import java.util.UUID;

public record AlertaPayload(
        UUID alertaId,
        UUID pacienteId,
        String nombrePaciente,
        String tipo,
        String mensaje,
        String nivel,
        long timestamp
) {}
