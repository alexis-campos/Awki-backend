package com.awki.riesgo.repository;

import com.awki.riesgo.entity.EvaluacionRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluacionRiesgoRepository extends JpaRepository<EvaluacionRiesgo, UUID> {

    List<EvaluacionRiesgo> findByEmbarazoIdOrderByCreatedAtDesc(UUID embarazoId);

    Optional<EvaluacionRiesgo> findTopByEmbarazoIdOrderByCreatedAtDesc(UUID embarazoId);
}
