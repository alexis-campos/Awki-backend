package com.awki.riesgo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReporteSintomaRequest(
    @NotNull(message = "El embarazoId es obligatorio")
    UUID embarazoId,
    
    @NotBlank(message = "El bienestar es obligatorio")
    String bienestar,
    
    @NotBlank(message = "Los movimientos fetales son obligatorios")
    String movimientos,
    
    @NotBlank(message = "El campo hinchazon es obligatorio")
    String hinchazon,
    
    String sintomasDetalle,
    
    String notas,
    
    List<TipoSintoma> sintomasClinicos
) {}
