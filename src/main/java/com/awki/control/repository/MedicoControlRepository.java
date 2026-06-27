package com.awki.control.repository;

import com.awki.auth.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicoControlRepository extends JpaRepository<Medico, UUID> {

    Optional<Medico> findByUsuarioId(UUID usuarioId);
}