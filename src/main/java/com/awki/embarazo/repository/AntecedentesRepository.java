package com.awki.embarazo.repository;

import com.awki.embarazo.entity.AntecedentesClinicos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AntecedentesRepository extends JpaRepository<AntecedentesClinicos, UUID> {
}
