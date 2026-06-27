package com.awki.vinculacion.dto;

import com.awki.common.enums.RolUsuario;

import java.time.LocalDateTime;

public record GenerarCodigoResponse(
        String codigo,
        LocalDateTime expiraAt,
        RolUsuario generadoPor
) {}
