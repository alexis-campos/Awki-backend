package com.awki.clinica.dto;

import com.awki.common.enums.EstadoSuscripcion;
import com.awki.common.enums.PlanSaas;
import com.awki.common.enums.TipoClinica;

import java.math.BigDecimal;
import java.util.UUID;

public record ClinicaResponse(
        UUID id,
        String nombre,
        TipoClinica tipo,
        String ruc,
        String diresa,
        String departamento,
        String provincia,
        String distrito,
        BigDecimal latitud,
        BigDecimal longitud,
        PlanSaas planSaas,
        EstadoSuscripcion estadoSuscripcion,
        Integer maxPacientes,
        Integer maxMedicos
) {}
