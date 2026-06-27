package com.awki.chat.dto;

import java.util.UUID;

public record UsuarioAutenticado(
        UUID id,
        String rol,
        UUID clinicaId
) {}
