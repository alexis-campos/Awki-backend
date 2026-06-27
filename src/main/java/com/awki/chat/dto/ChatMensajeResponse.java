package com.awki.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMensajeResponse(
        UUID mensajePacienteId,
        UUID mensajeIaId,
        String respuesta,
        boolean alarmaProbable,
        boolean desdeCache,
        boolean fallbackUsado,
        LocalDateTime timestamp
) {}
