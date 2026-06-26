package com.awki.riesgo.entity;

import com.awki.common.BaseEntity;
import com.awki.common.enums.NivelRiesgo;
import com.awki.riesgo.dto.FuenteEvaluacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "evaluaciones_riesgo")
public class EvaluacionRiesgo extends BaseEntity {

    @Column(name = "embarazo_id", nullable = false)
    private UUID embarazoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelRiesgo nivel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criterios_activos", columnDefinition = "jsonb")
    private List<String> criteriosActivos;

    @Column(name = "descripcion_alerta", columnDefinition = "text")
    private String descripcionAlerta;

    @Column(name = "generar_alerta", nullable = false)
    private boolean generarAlerta;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_evaluacion", nullable = false)
    private FuenteEvaluacion fuenteEvaluacion;
}
