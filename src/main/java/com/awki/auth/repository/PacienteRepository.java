package com.awki.auth.repository;

import com.awki.auth.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PacienteRepository extends JpaRepository<Paciente, UUID> {
    Optional<Paciente> findByUsuario_Id(UUID usuarioId);
}
