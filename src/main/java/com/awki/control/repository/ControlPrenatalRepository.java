package com.awki.control.repository;

import com.awki.control.entity.ControlPrenatal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ControlPrenatalRepository extends JpaRepository<ControlPrenatal, UUID> {

    List<ControlPrenatal> findByEmbarazo_IdOrderByFechaControlDesc(UUID embarazoId);

    long countByEmbarazo_Id(UUID embarazoId);
}
