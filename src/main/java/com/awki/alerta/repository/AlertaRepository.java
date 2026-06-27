package com.awki.alerta.repository;

import com.awki.alerta.entity.Alerta;
import com.awki.alerta.entity.EstadoEntrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, UUID> {
    Page<Alerta> findByMedico_IdOrderByNivelUrgenciaDescCreatedAtDesc(UUID medicoId, Pageable pageable);
    Page<Alerta> findByClinica_IdOrderByNivelUrgenciaDescCreatedAtDesc(UUID clinicaId, Pageable pageable);
    long countByMedico_IdAndVistaPorMedicoFalse(UUID medicoId);
    Optional<Alerta> findByIdAndMedico_Id(UUID id, UUID medicoId);
    List<Alerta> findByEstadoEntrega(EstadoEntrega estadoEntrega);
}
