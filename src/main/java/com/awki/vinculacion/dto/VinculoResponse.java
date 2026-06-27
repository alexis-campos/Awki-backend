package com.awki.vinculacion.dto;

import com.awki.common.enums.EstadoVinculo;

import java.time.LocalDateTime;
import java.util.UUID;

public record VinculoResponse(
        UUID id,
        UUID medicoId,
        UUID pacienteId,
        UUID clinicaId,
        EstadoVinculo estado,
        LocalDateTime vinculadoAt,
        LocalDateTime finalizadoAt,
        String pacienteNombres,
        String pacienteApellidos,
        String pacienteDni,
        Integer pacienteEdad,
        String medicoNombres,
        String medicoApellidos
) {}
