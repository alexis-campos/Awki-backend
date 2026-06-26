package com.awki.embarazo.repository;

import com.awki.common.enums.EstadoEmbarazo;
import com.awki.embarazo.entity.Embarazo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmbarazoRepository extends JpaRepository<Embarazo, UUID> {
    Optional<Embarazo> findByPacienteIdAndEstado(UUID pacienteId, EstadoEmbarazo estado);
    List<Embarazo> findByPacienteId(UUID pacienteId);
}
