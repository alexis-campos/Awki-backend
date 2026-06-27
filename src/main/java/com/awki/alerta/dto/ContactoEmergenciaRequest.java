package com.awki.alerta.dto;

import com.awki.alerta.entity.CanalPreferido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactoEmergenciaRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre,

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    String telefono,

    @Size(max = 50, message = "El parentesco no puede superar los 50 caracteres")
    String parentesco,

    @NotNull(message = "El canal preferido es obligatorio")
    CanalPreferido canalPreferido
) {}
