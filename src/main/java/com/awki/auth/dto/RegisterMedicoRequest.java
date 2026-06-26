package com.awki.auth.dto;

import com.awki.common.enums.Especialidad;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterMedicoRequest(
        @NotBlank(message = "El email es requerido")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @NotBlank(message = "Los nombres son requeridos")
        String nombres,

        @NotBlank(message = "Los apellidos son requeridos")
        String apellidos,

        @NotBlank(message = "El CMP es requerido")
        String cmp,

        Especialidad especialidad
) {}
