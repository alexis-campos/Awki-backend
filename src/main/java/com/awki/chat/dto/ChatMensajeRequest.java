package com.awki.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChatMensajeRequest(
        @NotNull(message = "El ID del embarazo es obligatorio")
        UUID embarazoId,

        @NotNull(message = "El contenido del mensaje no puede ser nulo")
        @Size(min = 2, max = 2000, message = "El contenido debe tener entre 2 y 2000 caracteres")
        String contenido
) {}
