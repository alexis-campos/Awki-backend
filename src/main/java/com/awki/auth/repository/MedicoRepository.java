package com.awki.auth.repository;

import com.awki.auth.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicoRepository extends JpaRepository<Medico, UUID> {
    boolean existsByCmp(String cmp);
    
    List<Medico> findByClinicaId(UUID clinicaId);
    
    @Query("SELECT COUNT(m) FROM Medico m WHERE m.clinicaId = :clinicaId AND m.usuario.activo = true")
    long countActiveByClinicaId(UUID clinicaId);

    Optional<Medico> findByUsuario_Id(UUID usuarioId);
}
