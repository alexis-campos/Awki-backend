package com.awki.riesgo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReporteSintomaResponse(
    UUID id,
    UUID embarazoId,
    String bienestar,
    String movimientos,
    String hinchazon,
    String sintomasDetalle,
    String notas,
    boolean esCritico,
    List<String> sintomasClinicos,
    LocalDateTime fecha
) {}
