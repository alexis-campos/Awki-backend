package com.awki.documento.repository;

import com.awki.documento.entity.DocumentoClinico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<DocumentoClinico, UUID> {

    Page<DocumentoClinico> findByEmbarazo_IdAndEliminadoFalse(UUID embarazoId, Pageable pageable);

    Optional<DocumentoClinico> findByIdAndEliminadoFalse(UUID id);
}