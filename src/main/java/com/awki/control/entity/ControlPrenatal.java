package com.awki.control.entity;

import com.awki.common.BaseEntity;
import com.awki.common.enums.NivelRiesgo;
import com.awki.embarazo.entity.Embarazo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "controles_prenatales")
public class ControlPrenatal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embarazo_id", nullable = false)
    private Embarazo embarazo;

    @Column(name = "medico_id", nullable = false)
    private UUID medicoId;

    @Column(name = "numero_control", nullable = false)
    private Integer numeroControl;

    @Column(name = "fecha_control", nullable = false)
    private LocalDate fechaControl;

    @Column(name = "semanas_gestacion", nullable = false)
    private Integer semanasGestacion;

    @Column(name = "peso_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "talla_cm", precision = 5, scale = 1)
    private BigDecimal tallaCm;

    @Column(name = "imc", precision = 4, scale = 2)
    private BigDecimal imc;

    @Column(name = "presion_arterial_sistolica", nullable = false)
    private Integer presionArterialSistolica;

    @Column(name = "presion_arterial_diastolica", nullable = false)
    private Integer presionArterialDiastolica;

    @Column(name = "altura_uterina_cm", precision = 4, scale = 1)
    private BigDecimal alturaUterinaCm;

    @Column(name = "frecuencia_cardiaca_fetal")
    private Integer frecuenciaCardiacaFetal;

    @Enumerated(EnumType.STRING)
    @Column(name = "presentacion_fetal")
    private PresentacionFetal presentacionFetal;

    @Column(name = "hemoglobina_gdl", precision = 4, scale = 2)
    private BigDecimal hemoglobinaGdl;

    @Enumerated(EnumType.STRING)
    @Column(name = "proteinuria")
    private ProteinuriaControl proteinuria;

    @Column(name = "glucosa_mgdl", precision = 5, scale = 2)
    private BigDecimal glucosaMgdl;

    @Enumerated(EnumType.STRING)
    @Column(name = "movimientos_fetales_reporte")
    private MovimientosFetalesControl movimientosFetalesReporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "edemas")
    private EdemasControl edemas;

    @Column(name = "proxima_cita")
    private LocalDate proximaCita;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo_calculado", nullable = false)
    private NivelRiesgo nivelRiesgoCalculado;

    @Column(name = "observaciones_medico", columnDefinition = "text")
    private String observacionesMedico;
}
