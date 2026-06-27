package com.awki.alerta.repository;

import com.awki.alerta.entity.ContactoEmergencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactoEmergenciaRepository extends JpaRepository<ContactoEmergencia, UUID> {
    List<ContactoEmergencia> findByPaciente_IdAndActivoTrue(UUID pacienteId);
}
