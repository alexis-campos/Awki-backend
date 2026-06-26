package com.awki.clinica.dto;

import java.util.UUID;

public record PacienteResumenResponse(
        UUID id,
        String nombres,
        String apellidos,
        String dni,
        String medicoAsignado,
        String riesgoActual
) {}
