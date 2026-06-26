package com.awki.vinculacion.service;

import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class VinculacionService {

    /**
     * Obtiene el ID del médico vinculado activo a la paciente.
     * TODO: MODULO_VINCULACION - Reemplazar con consulta real a la tabla vinculos_medico_paciente cuando Diego lo implemente.
     */
    public Optional<UUID> obtenerMedicoVinculadoActivo(UUID pacienteId) {
        return Optional.empty();
    }
}
