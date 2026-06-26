package com.awki.embarazo.dto;

import java.util.UUID;

public record AntecedentesResponse(
        UUID embarazoId,
        boolean diabetesPrevia,
        boolean hipertensionPrevia,
        boolean preeclampsiaPrevia,
        boolean enfermedadRenal,
        boolean enfermedadAutoinmune,
        boolean anemiaPrevia,
        boolean vihPositivo,
        boolean sifilisPrevia,
        boolean trastornoCoagulacion,
        boolean edadMaternaRiesgo,
        boolean residenciaAltitud,
        boolean obesidadPregestacional,
        String observaciones
) {}
