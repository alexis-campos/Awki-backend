package com.awki.embarazo.dto;

public record AntecedentesRequest(
        Boolean diabetesPrevia,
        Boolean hipertensionPrevia,
        Boolean preeclampsiaPrevia,
        Boolean enfermedadRenal,
        Boolean enfermedadAutoinmune,
        Boolean anemiaPrevia,
        Boolean vihPositivo,
        Boolean sifilisPrevia,
        Boolean trastornoCoagulacion,
        Boolean residenciaAltitud,
        Boolean obesidadPregestacional,
        String observaciones
) {}
