package com.awki.epicrisis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EpicrisisGenerarRequest(
        @NotNull(message = "El embarazoId es obligatorio")
        UUID embarazoId,

        @NotBlank(message = "El motivo de derivación es obligatorio")
        @Size(max = 1000, message = "El motivo de derivación no puede superar los 1000 caracteres")
        String motivoDerivacion,

        @Size(max = 2000, message = "Las observaciones adicionales no pueden superar los 2000 caracteres")
        String observacionesAdicionales
) {}
