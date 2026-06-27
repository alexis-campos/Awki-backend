package com.awki.epicrisis.entity;

import com.awki.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "epicrisis")
@Getter
@Setter
public class Epicrisis extends BaseEntity {

    @Column(name = "embarazo_id", nullable = false)
    private UUID embarazoId;

    @Column(name = "medico_id", nullable = false)
    private UUID medicoId;

    @Column(name = "clinica_id", nullable = false)
    private UUID clinicaId;

    @Column(name = "motivo_derivacion", length = 1000, nullable = false)
    private String motivoDerivacion;

    @Column(name = "observaciones_adicionales", length = 2000)
    private String observacionesAdicionales;

    @Column(name = "contenido_json", columnDefinition = "TEXT", nullable = false)
    private String contenidoJson;

    @Column(name = "url_pdf", length = 500, nullable = false)
    private String urlPdf;

    @Column(nullable = false)
    private boolean activo = true;
}
