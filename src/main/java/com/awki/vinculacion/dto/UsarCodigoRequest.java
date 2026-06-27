package com.awki.vinculacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UsarCodigoRequest(
        @NotBlank(message = "El código es requerido")
        @Pattern(
                regexp = "^[A-HJ-NP-Za-hj-np-z2-9]{8}$",
                message = "El código debe tener 8 caracteres alfanuméricos válidos"
        )
        String codigo
) {}
