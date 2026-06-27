package com.awki.alerta.repository;

import com.awki.alerta.entity.DispositivoMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DispositivoMedicoRepository extends JpaRepository<DispositivoMedico, UUID> {
    List<DispositivoMedico> findByMedico_Id(UUID medicoId);
    Optional<DispositivoMedico> findByMedico_IdAndFcmToken(UUID medicoId, String fcmToken);
}
