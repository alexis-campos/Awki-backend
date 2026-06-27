package com.awki.control.dto;

import com.awki.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ControlPrenatalResumenResponse(
        UUID controlId,
        LocalDate fechaControl,
        Integer semanasGestacion,
        Integer numeroControl,
        NivelRiesgo nivelRiesgoCalculado,
        String presionArterial,
        BigDecimal pesoKg,
        BigDecimal hemoglobinaGdl
) {}