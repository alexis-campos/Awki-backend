package com.awki.chat.entity;

import com.awki.embarazo.entity.Embarazo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "resumenes_clinicos")
@EntityListeners(AuditingEntityListener.class)
public class ResumenClinico implements Persistable<UUID> {

    @Id
    @Column(name = "embarazo_id")
    private UUID embarazoId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "embarazo_id")
    private Embarazo embarazo;

    @Column(name = "contenido_resumen", nullable = false, columnDefinition = "TEXT")
    private String contenidoResumen;

    @Column(name = "generado_por_modelo", length = 100)
    private String generadoPorModelo;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private boolean newEntity;

    @Override
    public UUID getId() {
        return embarazoId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }
}
