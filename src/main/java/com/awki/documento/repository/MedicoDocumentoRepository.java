package com.awki.documento.repository;

import com.awki.auth.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicoDocumentoRepository extends JpaRepository<Medico, UUID> {

    Optional<Medico> findByUsuarioId(UUID usuarioId);
}