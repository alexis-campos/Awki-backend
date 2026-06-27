package com.awki.riesgo.repository;

import com.awki.riesgo.entity.ReporteSintoma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReporteSintomaRepository extends JpaRepository<ReporteSintoma, UUID> {
    List<ReporteSintoma> findByEmbarazoIdOrderByCreatedAtDesc(UUID embarazoId);
}
