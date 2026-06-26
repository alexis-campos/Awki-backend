package com.awki.auth.entity;

import com.awki.common.BaseEntity;
import com.awki.common.enums.Especialidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "medicos")
public class Medico extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Referencia por ID para respetar los límites del módulo (no @ManyToOne a clinica module)
    @Column(name = "clinica_id", nullable = false)
    private UUID clinicaId;

    private String nombres;
    private String apellidos;

    @Column(unique = true, nullable = false)
    private String cmp;

    @Enumerated(EnumType.STRING)
    private Especialidad especialidad;
}
