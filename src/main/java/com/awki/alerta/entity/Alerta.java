package com.awki.alerta.entity;

import com.awki.common.BaseEntity;
import com.awki.embarazo.entity.Embarazo;
import com.awki.auth.entity.Paciente;
import com.awki.auth.entity.Medico;
import com.awki.clinica.entity.Clinica;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "alertas")
public class Alerta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embarazo_id", nullable = false)
    private Embarazo embarazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alerta", nullable = false)
    private TipoAlerta tipoAlerta;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_urgencia", nullable = false)
    private NivelUrgencia nivelUrgencia;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sintomas_disparadores", columnDefinition = "jsonb")
    private List<String> sintomasDisparadores;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenAlerta origen;

    @Column(name = "vista_por_medico", nullable = false)
    private boolean vistaPorMedico = false;

    @Column(name = "fecha_vista")
    private LocalDateTime fechaVista;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false)
    private EstadoEntrega estadoEntrega = EstadoEntrega.PENDIENTE;

    @Column(name = "websocket_enviado", nullable = false)
    private boolean websocketEnviado = false;

    @Column(name = "sms_enviado", nullable = false)
    private boolean smsEnviado = false;

    @Column(name = "whatsapp_enviado", nullable = false)
    private boolean whatsappEnviado = false;

    @Column(name = "fcm_enviado", nullable = false)
    private boolean fcmEnviado = false;

    @Column(name = "intentos_sms", nullable = false)
    private int intentosSms = 0;

    @Column(name = "intentos_whatsapp", nullable = false)
    private int intentosWhatsapp = 0;

    @Column(name = "intentos_fcm", nullable = false)
    private int intentosFcm = 0;

    private BigDecimal latitud;
    private BigDecimal longitud;

    @Column(name = "mensaje_libre", length = 500)
    private String mensajeLibre;
}
