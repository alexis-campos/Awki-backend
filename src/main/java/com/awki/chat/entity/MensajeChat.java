package com.awki.chat.entity;

import com.awki.common.BaseEntity;
import com.awki.embarazo.entity.Embarazo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "mensajes_chat")
public class MensajeChat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embarazo_id", nullable = false)
    private Embarazo embarazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolMensajeChat rol;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "alarma_probable", nullable = false)
    private boolean alarmaProbable = false;

    @Column(name = "desde_cache", nullable = false)
    private boolean desdeCache = false;

    @Column(name = "fallback_usado", nullable = false)
    private boolean fallbackUsado = false;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "offline_timestamp")
    private LocalDateTime offlineTimestamp;
}
