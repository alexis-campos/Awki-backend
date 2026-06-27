package com.awki.vinculacion.repository;

import com.awki.common.enums.EstadoVinculo;
import com.awki.vinculacion.entity.VinculoMedicoPaciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VinculoRepository extends JpaRepository<VinculoMedicoPaciente, UUID> {

    boolean existsByPaciente_IdAndEstado(UUID pacienteId, EstadoVinculo estado);

    Optional<VinculoMedicoPaciente> findByPaciente_IdAndEstado(UUID pacienteId, EstadoVinculo estado);

    List<VinculoMedicoPaciente> findByMedico_IdAndEstado(UUID medicoId, EstadoVinculo estado);
}
