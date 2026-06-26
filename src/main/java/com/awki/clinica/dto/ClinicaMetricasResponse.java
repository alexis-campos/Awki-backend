package com.awki.clinica.dto;

public record ClinicaMetricasResponse(
        long pacientesActivas,
        long controlesMes,
        long alertasRojasMes,
        double usoPlanPorcentaje
) {}
