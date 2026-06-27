package com.awki.vinculacion.dto;

import com.awki.common.enums.RolUsuario;

import java.util.UUID;

public record CodigoVinculacionData(
        UUID generadoPor,
        RolUsuario rol,
        UUID clinicaId
) {}
