package com.awki.alerta.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record SosRequest(
    @NotNull(message = "El ID de embarazo es obligatorio")
    UUID embarazoId,
    
    BigDecimal latitud,
    
    BigDecimal longitud,
    
    @Size(max = 500, message = "El mensaje libre no puede superar los 500 caracteres")
    String mensajeLibre
) {}
