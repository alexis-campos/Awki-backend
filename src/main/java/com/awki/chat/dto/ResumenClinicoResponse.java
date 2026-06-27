package com.awki.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResumenClinicoResponse(
        UUID embarazoId,
        String contenidoResumen,
        String generadoPorModelo,
        LocalDateTime updatedAt
) {}
