package com.awki.clinica.entity;

import com.awki.common.BaseEntity;
import com.awki.common.enums.EstadoSuscripcion;
import com.awki.common.enums.PlanSaas;
import com.awki.common.enums.TipoClinica;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "clinicas")
public class Clinica extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoClinica tipo;

    @Column(nullable = false, unique = true)
    private String ruc;

    private String diresa;

    @Column(nullable = false)
    private String departamento;

    @Column(nullable = false)
    private String provincia;

    @Column(nullable = false)
    private String distrito;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_saas", nullable = false)
    private PlanSaas planSaas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_suscripcion", nullable = false)
    private EstadoSuscripcion estadoSuscripcion;

    @Column(name = "max_pacientes", nullable = false)
    private Integer maxPacientes;

    @Column(name = "max_medicos", nullable = false)
    private Integer maxMedicos;
}
