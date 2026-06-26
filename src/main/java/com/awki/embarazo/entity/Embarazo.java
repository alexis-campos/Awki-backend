package com.awki.embarazo.entity;

import com.awki.common.BaseEntity;
import com.awki.common.enums.EstadoEmbarazo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "embarazos")
public class Embarazo extends BaseEntity {

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "medico_id")
    private UUID medicoId;

    @Column(name = "fecha_ultima_menstruacion", nullable = false)
    private LocalDate fechaUltimaMenstruacion;

    @Column(name = "fecha_probable_parto", nullable = false)
    private LocalDate fechaProbableParto;

    @Column(name = "fecha_probable_parto_eco")
    private LocalDate fechaProbablePartoEco;

    @Column(name = "semanas_gestacion_ingreso", nullable = false)
    private Integer semanasGestacionIngreso;

    @Column(name = "numero_gestacion", nullable = false)
    private Integer numeroGestacion;

    @Column(name = "numero_partos", nullable = false)
    private Integer numeroPartos = 0;

    @Column(name = "numero_abortos", nullable = false)
    private Integer numeroAbortos = 0;

    @Column(name = "numero_cesareas", nullable = false)
    private Integer numeroCesareas = 0;

    @Column(name = "embarazo_multiple", nullable = false)
    private boolean embarazoMultiple = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEmbarazo estado;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @OneToOne(mappedBy = "embarazo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AntecedentesClinicos antecedentes;
}
