package com.awki.chat.repository;

import com.awki.chat.entity.ResumenClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResumenClinicoRepository extends JpaRepository<ResumenClinico, UUID> {
}
