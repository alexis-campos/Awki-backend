package com.awki.epicrisis.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EpicrisisResponse(
        UUID id,
        UUID embarazoId,
        UUID medicoId,
        UUID clinicaId,
        String motivoDerivacion,
        String observacionesAdicionales,
        String contenidoJson,
        String urlPdf,
        LocalDateTime createdAt
) {}
