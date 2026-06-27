package com.awki.clinica.service;

import com.awki.auth.dto.MedicoInfoDto;
import com.awki.auth.service.AuthService;
import com.awki.clinica.dto.*;
import com.awki.clinica.entity.Clinica;
import com.awki.clinica.repository.ClinicaRepository;
import com.awki.exception.BusinessRuleException;
import com.awki.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicaService {

    private final ClinicaRepository clinicaRepository;
    private final AuthService authService;

    public ClinicaResponse getClinicaById(UUID id) {
        Clinica clinica = clinicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica", id.toString()));
        return mapToClinicaResponse(clinica);
    }

    public List<ClinicaMedicoResponse> getMedicosByClinica(UUID clinicaId) {
        // Verificar que la clínica existe
        if (!clinicaRepository.existsById(clinicaId)) {
            throw new ResourceNotFoundException("Clínica", clinicaId.toString());
        }

        List<MedicoInfoDto> medicos = authService.getMedicosByClinicaId(clinicaId);
        return medicos.stream()
                .map(m -> new ClinicaMedicoResponse(
                        m.id(),
                        m.nombres(),
                        m.apellidos(),
                        m.cmp(),
                        m.especialidad() != null ? m.especialidad().name() : null,
                        m.activo(),
                        // TODO: MODULO_VINCULACION - Reemplazar con el conteo real de pacientes vinculadas a este médico
                        5L
                ))
                .toList();
    }

    public void cambiarEstadoMedico(UUID clinicaId, UUID medicoId, boolean activo) {
        // Delegamos la actualización al módulo Auth, que validará la pertenencia del médico a la clínica
        authService.setMedicoActivo(clinicaId, medicoId, activo);
    }

    public ClinicaMetricasResponse getMetricas(UUID clinicaId) {
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica", clinicaId.toString()));

        // TODO: MODULO_VINCULACION - Reemplazar con conteo real de pacientes vinculadas activas a la clínica
        long pacientesActivas = 12L;

        // TODO: MODULO_CONTROLES - Reemplazar con conteo real de controles realizados en el mes en curso
        long controlesMes = 45L;

        // TODO: MODULO_ALERTAS - Reemplazar con conteo real de alertas rojas generadas en el mes
        long alertasRojasMes = 3L;

        double usoPlanPorcentaje = (pacientesActivas * 100.0) / clinica.getMaxPacientes();

        return new ClinicaMetricasResponse(pacientesActivas, controlesMes, alertasRojasMes, usoPlanPorcentaje);
    }

    public List<PacienteResumenResponse> getPacientesResumen(UUID clinicaId) {
        if (!clinicaRepository.existsById(clinicaId)) {
            throw new ResourceNotFoundException("Clínica", clinicaId.toString());
        }

        // TODO: MODULO_VINCULACION - Reemplazar con consulta real a la tabla de vínculos y pacientes para esta clínica
        // TODO: MODULO_ALERTAS/RIESGO - Obtener el riesgo actual real calculado por el motor de riesgo
        return List.of(
                new PacienteResumenResponse(UUID.randomUUID(), "María", "López", "12345678", "Dr. Carlos Ramirez", "ROJO"),
                new PacienteResumenResponse(UUID.randomUUID(), "Ana", "Gómez", "87654321", "Dra. Laura Torres", "AMARILLO"),
                new PacienteResumenResponse(UUID.randomUUID(), "Lucía", "Díaz", "45678912", "Dr. Carlos Ramirez", "VERDE")
        );
    }

    public void verificarLimiteMedicos(UUID clinicaId) {
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica", clinicaId.toString()));

        long medicosActivos = authService.countActiveMedicosByClinicaId(clinicaId);
        if (medicosActivos >= clinica.getMaxMedicos()) {
            throw new BusinessRuleException("TENANT_LIMIT_REACHED", "La clínica ha alcanzado el límite máximo de médicos permitidos en su plan.");
        }
    }

    public void verificarLimitePacientes(UUID clinicaId) {
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica", clinicaId.toString()));

        // TODO: MODULO_VINCULACION - Reemplazar con conteo real de pacientes vinculadas activas en la clínica
        long pacientesActivas = 12L;
        if (pacientesActivas >= clinica.getMaxPacientes()) {
            throw new BusinessRuleException("TENANT_LIMIT_REACHED", "La clínica ha alcanzado el límite máximo de pacientes permitidos en su plan.");
        }
    }

    public Clinica getClinicaEntityById(UUID id) {
        return clinicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica", id.toString()));
    }

    private ClinicaResponse mapToClinicaResponse(Clinica clinica) {
        return new ClinicaResponse(
                clinica.getId(),
                clinica.getNombre(),
                clinica.getTipo(),
                clinica.getRuc(),
                clinica.getDiresa(),
                clinica.getDepartamento(),
                clinica.getProvincia(),
                clinica.getDistrito(),
                clinica.getLatitud(),
                clinica.getLongitud(),
                clinica.getPlanSaas(),
                clinica.getEstadoSuscripcion(),
                clinica.getMaxPacientes(),
                clinica.getMaxMedicos()
        );
    }
}
