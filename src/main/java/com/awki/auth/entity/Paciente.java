package com.awki.auth.entity;

import com.awki.common.BaseEntity;
import com.awki.common.enums.ModoUso;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "pacientes")
public class Paciente extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String nombres;
    private String apellidos;
    private String telefono;
    private String dni;
    private LocalDate fechaNacimiento;
    private String departamento;

    @Enumerated(EnumType.STRING)
    private ModoUso modoUso;

    private boolean consentimientoIa;
    private LocalDateTime consentimientoFecha;
}
