package com.awki.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterPacienteRequest(
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

        String telefono,
        String dni,

        @NotNull(message = "La fecha de nacimiento es requerida")
        LocalDate fechaNacimiento,

        String departamento,

        // Validado en el service: null y false lanzan ConsentRequiredException (HTTP 422)
        Boolean consentimientoIa
) {}
