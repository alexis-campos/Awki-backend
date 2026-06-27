package com.awki.control.dto;

import com.awki.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ControlPrenatalResponse(
        UUID controlId,
        NivelRiesgo nivelRiesgoCalculado,
        List<String> alertasGeneradas,
        BigDecimal imcCalculado,
        Integer semanasGestacion
) {}