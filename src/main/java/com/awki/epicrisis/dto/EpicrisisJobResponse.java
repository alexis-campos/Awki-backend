package com.awki.epicrisis.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EpicrisisJobResponse(
        UUID id,
        UUID embarazoId,
        UUID medicoId,
        UUID clinicaId,
        String estado,
        UUID epicrisisId,
        String mensajeError,
        String urlPdf,
        LocalDateTime createdAt
) {}
