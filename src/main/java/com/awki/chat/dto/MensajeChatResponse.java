package com.awki.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MensajeChatResponse(
        UUID id,
        UUID embarazoId,
        String rol,
        String contenido,
        boolean alarmaProbable,
        boolean desdeCache,
        boolean fallbackUsado,
        LocalDateTime createdAt
) {}
