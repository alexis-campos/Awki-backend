package com.awki.sync.repository;

import com.awki.sync.entity.DispositivoSync;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DispositivoSyncRepository extends JpaRepository<DispositivoSync, UUID> {
    Optional<DispositivoSync> findByPacienteIdAndDeviceId(UUID pacienteId, String deviceId);
}
