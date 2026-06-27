package com.awki.sync.entity;

import com.awki.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "sync_items_procesados")
public class SyncItemProcesado extends BaseEntity {

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "offline_timestamp", nullable = false)
    private LocalDateTime offlineTimestamp;
}
