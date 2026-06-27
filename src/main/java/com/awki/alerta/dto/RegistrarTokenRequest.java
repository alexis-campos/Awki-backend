package com.awki.alerta.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrarTokenRequest(
    @NotBlank(message = "El fcmToken es obligatorio")
    String fcmToken
) {}
