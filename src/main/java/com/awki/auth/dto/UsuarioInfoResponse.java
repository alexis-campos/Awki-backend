package com.awki.auth.dto;

import com.awki.common.enums.RolUsuario;
import java.util.UUID;

public record UsuarioInfoResponse(
        UUID id,
        String email,
        RolUsuario rol,
        UUID perfilId,
        String nombres,
        String apellidos
) {}
