package com.awki.sync.repository;

import com.awki.sync.entity.SyncItemProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SyncItemProcesadoRepository extends JpaRepository<SyncItemProcesado, UUID> {
    boolean existsByDeviceIdAndPacienteIdAndOfflineTimestamp(String deviceId, UUID pacienteId, LocalDateTime offlineTimestamp);
}
