package com.awki.alerta.entity;

import com.awki.common.BaseEntity;
import com.awki.auth.entity.Paciente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "contactos_emergencia")
public class ContactoEmergencia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 50)
    private String parentesco;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_preferido", nullable = false, length = 30)
    private CanalPreferido canalPreferido;

    @Column(nullable = false)
    private boolean activo = true;
}
