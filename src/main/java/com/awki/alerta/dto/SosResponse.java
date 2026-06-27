package com.awki.alerta.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SosResponse(
    UUID alertaId,
    String nivel,
    int contactosNotificados,
    boolean medicoNotificado,
    LocalDateTime timestamp
) {}
