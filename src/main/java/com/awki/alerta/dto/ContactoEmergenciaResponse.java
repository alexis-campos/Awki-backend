package com.awki.alerta.dto;

import java.util.UUID;

public record ContactoEmergenciaResponse(
    UUID id,
    String nombre,
    String telefono,
    String parentesco,
    String canalPreferido,
    boolean activo
) {}
