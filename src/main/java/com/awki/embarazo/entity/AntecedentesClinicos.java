package com.awki.embarazo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "antecedentes_clinicos")
@EntityListeners(AuditingEntityListener.class)
public class AntecedentesClinicos {

    @Id
    @Column(name = "embarazo_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "embarazo_id")
    private Embarazo embarazo;

    @Column(name = "diabetes_previa", nullable = false)
    private boolean diabetesPrevia = false;

    @Column(name = "hipertension_previa", nullable = false)
    private boolean hipertensionPrevia = false;

    @Column(name = "preeclampsia_previa", nullable = false)
    private boolean preeclampsiaPrevia = false;

    @Column(name = "enfermedad_renal", nullable = false)
    private boolean enfermedadRenal = false;

    @Column(name = "enfermedad_autoinmune", nullable = false)
    private boolean enfermedadAutoinmune = false;

    @Column(name = "anemia_previa", nullable = false)
    private boolean anemiaPrevia = false;

    @Column(name = "vih_positivo", nullable = false)
    private boolean vihPositivo = false;

    @Column(name = "sifilis_previa", nullable = false)
    private boolean sifilisPrevia = false;

    @Column(name = "trastorno_coagulacion", nullable = false)
    private boolean trastornoCoagulacion = false;

    @Column(name = "edad_materna_riesgo", nullable = false)
    private boolean edadMaternaRiesgo = false;

    @Column(name = "residencia_altitud", nullable = false)
    private boolean residenciaAltitud = false;

    @Column(name = "obesidad_pregestacional", nullable = false)
    private boolean obesidadPregestacional = false;

    private String observaciones;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
