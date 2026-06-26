package com.awki.clinica.dto;

import java.util.UUID;

public record ClinicaMedicoResponse(
        UUID id,
        String nombres,
        String apellidos,
        String cmp,
        String especialidad,
        boolean activo,
        long pacientesVinculadas
) {}
