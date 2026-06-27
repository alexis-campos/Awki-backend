package com.awki.epicrisis.repository;

import com.awki.epicrisis.entity.Epicrisis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EpicrisisRepository extends JpaRepository<Epicrisis, UUID> {
}
