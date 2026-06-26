package com.awki.auth.repository;

import com.awki.auth.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MedicoRepository extends JpaRepository<Medico, UUID> {
    boolean existsByCmp(String cmp);
}
