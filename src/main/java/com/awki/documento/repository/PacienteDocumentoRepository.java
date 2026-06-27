package com.awki.documento.repository;

import com.awki.auth.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PacienteDocumentoRepository extends JpaRepository<Paciente, UUID> {

    Optional<Paciente> findByUsuarioId(UUID usuarioId);
}