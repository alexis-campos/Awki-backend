package com.awki.epicrisis.entity;

import com.awki.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "epicrisis_jobs")
@Getter
@Setter
public class EpicrisisJob extends BaseEntity {

    @Column(name = "embarazo_id", nullable = false)
    private UUID embarazoId;

    @Column(name = "medico_id", nullable = false)
    private UUID medicoId;

    @Column(name = "clinica_id", nullable = false)
    private UUID clinicaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoJob estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epicrisis_id")
    private Epicrisis epicrisis;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(nullable = false)
    private boolean activo = true;
}
