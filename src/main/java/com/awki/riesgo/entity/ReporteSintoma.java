package com.awki.riesgo.entity;

import com.awki.common.BaseEntity;
import com.awki.embarazo.entity.Embarazo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "reportes_sintomas")
public class ReporteSintoma extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embarazo_id", nullable = false)
    private Embarazo embarazo;

    @Column(nullable = false)
    private String bienestar;

    @Column(nullable = false)
    private String movimientos;

    @Column(nullable = false)
    private String hinchazon;

    @Column(name = "sintomas_detalle")
    private String sintomasDetalle;

    private String notas;

    @Column(name = "es_critico", nullable = false)
    private boolean esCritico = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sintomas_clinicos", columnDefinition = "jsonb")
    private List<String> sintomasClinicos;
}
