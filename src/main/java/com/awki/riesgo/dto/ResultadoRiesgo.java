package com.awki.riesgo.dto;

import com.awki.common.enums.NivelRiesgo;

import java.util.List;

public record ResultadoRiesgo(
        NivelRiesgo nivel,
        List<String> criteriosActivos,
        boolean generarAlerta,
        String descripcionAlerta
) {}
