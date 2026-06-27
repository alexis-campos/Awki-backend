package com.awki.alerta.entity;

import com.awki.common.BaseEntity;
import com.awki.auth.entity.Medico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dispositivos_medico", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"medico_id", "fcm_token"})
})
public class DispositivoMedico extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(name = "fcm_token", nullable = false)
    private String fcmToken;
}
