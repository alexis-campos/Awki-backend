package com.awki.vinculacion.entity;

import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.common.BaseEntity;
import com.awki.common.enums.EstadoVinculo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "vinculos_medico_paciente")

public class VinculoMedicoPaciente extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "clinica_id", nullable = false)
    private UUID clinicaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVinculo estado;

    @Column(name = "vinculado_at", nullable = false)
    private LocalDateTime vinculadoAt;

    @Column(name = "finalizado_at")
    private LocalDateTime finalizadoAt;
}
