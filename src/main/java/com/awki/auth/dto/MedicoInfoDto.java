package com.awki.auth.dto;

import com.awki.common.enums.Especialidad;

import java.util.UUID;

public record MedicoInfoDto(
        UUID id,
        UUID usuarioId,
        String nombres,
        String apellidos,
        String cmp,
        Especialidad especialidad,
        boolean activo
) {}
