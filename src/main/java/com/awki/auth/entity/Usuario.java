package com.awki.auth.entity;

import com.awki.common.BaseEntity;
import com.awki.common.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "clinica_id")
    private UUID clinicaId;
}
