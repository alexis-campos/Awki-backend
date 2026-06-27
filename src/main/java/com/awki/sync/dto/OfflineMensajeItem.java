package com.awki.sync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record OfflineMensajeItem(
        @NotNull UUID embarazoId,
        @NotBlank @Size(max = 5000) String contenido,
        @NotNull LocalDateTime offlineTimestamp
) {}
