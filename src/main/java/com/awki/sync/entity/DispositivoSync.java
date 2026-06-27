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
@Table(name = "dispositivos_sync")
public class DispositivoSync extends BaseEntity {

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "ultima_sincronizacion")
    private LocalDateTime ultimaSincronizacion;
}
