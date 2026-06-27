package com.awki.embarazo.service;

import com.awki.auth.service.AuthService;
import com.awki.common.enums.EstadoEmbarazo;
import com.awki.embarazo.dto.*;
import com.awki.embarazo.entity.AntecedentesClinicos;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.repository.AntecedentesRepository;
import com.awki.embarazo.repository.EmbarazoRepository;
import com.awki.exception.BusinessRuleException;
import com.awki.exception.ResourceNotFoundException;
import com.awki.vinculacion.service.VinculacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmbarazoService {

    private final EmbarazoRepository embarazoRepository;
    private final AntecedentesRepository antecedentesRepository;
    private final AuthService authService;
    private final VinculacionService vinculacionService;

    @Transactional
    public EmbarazoResponse crearEmbarazo(EmbarazoRequest request) {
        // Resolver pacienteId real si se proporcionó un usuarioId
        UUID targetPacienteId = authService.tryGetPacienteIdByUsuarioId(request.pacienteId())
                .orElse(request.pacienteId());

        // Verificar si la paciente ya tiene un embarazo activo
        final UUID finalPacienteId = targetPacienteId;
        embarazoRepository.findByPacienteIdAndEstado(targetPacienteId, EstadoEmbarazo.ACTIVO)
                .ifPresent(e -> {
                    throw new BusinessRuleException("ACTIVE_PREGNANCY_EXISTS", "La paciente ya tiene un embarazo activo registrado");
                });

        LocalDate hoy = LocalDate.now();
        long semanas = ChronoUnit.WEEKS.between(request.fechaUltimaMenstruacion(), hoy);
        if (semanas < 0 || semanas > 45) {
            throw new BusinessRuleException("INVALID_FUM", "La Fecha de Última Menstruación (FUM) no es coherente con un embarazo activo");
        }

        // FPP = FUM + 280 días (Regla de Naegele)
        LocalDate fpp = request.fechaUltimaMenstruacion().plusDays(280);

        Embarazo embarazo = new Embarazo();
        embarazo.setPacienteId(targetPacienteId);
        embarazo.setFechaUltimaMenstruacion(request.fechaUltimaMenstruacion());
        embarazo.setFechaProbableParto(fpp);
        embarazo.setFechaProbablePartoEco(request.fechaProbablePartoEco());
        embarazo.setSemanasGestacionIngreso((int) semanas);
        embarazo.setNumeroGestacion(request.numeroGestacion());
        embarazo.setNumeroPartos(request.numeroPartos() != null ? request.numeroPartos() : 0);
        embarazo.setNumeroAbortos(request.numeroAbortos() != null ? request.numeroAbortos() : 0);
        embarazo.setNumeroCesareas(request.numeroCesareas() != null ? request.numeroCesareas() : 0);
        embarazo.setEmbarazoMultiple(request.embarazoMultiple() != null ? request.embarazoMultiple() : false);
        embarazo.setEstado(EstadoEmbarazo.ACTIVO);

        // Obtener médico vinculado si está en modo VINCULADO (Lógica Módulo 2)
        vinculacionService.obtenerMedicoVinculadoActivo(targetPacienteId)
                .ifPresent(embarazo::setMedicoId);

        embarazo = embarazoRepository.save(embarazo);

        // Calcular edad materna de riesgo (edad < 15 o > 35)
        LocalDate fechaNacimiento = authService.getFechaNacimientoPaciente(targetPacienteId);
        long edad = ChronoUnit.YEARS.between(fechaNacimiento, hoy);
        boolean edadMaternaRiesgo = edad < 15 || edad > 35;

        // Crear antecedentes clínicos iniciales asociados al embarazo
        AntecedentesClinicos antecedentes = new AntecedentesClinicos();
        antecedentes.setEmbarazo(embarazo);
        antecedentes.setEdadMaternaRiesgo(edadMaternaRiesgo);
        
        // Carga opcional si vienen algunos antecedentes iniciales en la creación
        antecedentesRepository.save(antecedentes);
        embarazo.setAntecedentes(antecedentes);

        return mapToEmbarazoResponse(embarazo);
    }

    public EmbarazoResponse obtenerEmbarazoActivo(UUID pacienteId) {
        UUID targetPacienteId = authService.tryGetPacienteIdByUsuarioId(pacienteId)
                .orElse(pacienteId);
        final UUID finalPacienteId = targetPacienteId;
        Embarazo embarazo = embarazoRepository.findByPacienteIdAndEstado(finalPacienteId, EstadoEmbarazo.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo activo", finalPacienteId.toString()));
        return mapToEmbarazoResponse(embarazo);
    }

    public EmbarazoResponse obtenerEmbarazoPorId(UUID id) {
        Embarazo embarazo = embarazoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", id.toString()));
        return mapToEmbarazoResponse(embarazo);
    }

    @Transactional
    public AntecedentesResponse crearOActualizarAntecedentes(UUID embarazoId, AntecedentesRequest request) {
        Embarazo embarazo = embarazoRepository.findById(embarazoId)
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", embarazoId.toString()));

        AntecedentesClinicos antecedentes = embarazo.getAntecedentes();
        if (antecedentes == null) {
            antecedentes = new AntecedentesClinicos();
            antecedentes.setEmbarazo(embarazo);
        }

        if (request.diabetesPrevia() != null) antecedentes.setDiabetesPrevia(request.diabetesPrevia());
        if (request.hipertensionPrevia() != null) antecedentes.setHipertensionPrevia(request.hipertensionPrevia());
        if (request.preeclampsiaPrevia() != null) antecedentes.setPreeclampsiaPrevia(request.preeclampsiaPrevia());
        if (request.enfermedadRenal() != null) antecedentes.setEnfermedadRenal(request.enfermedadRenal());
        if (request.enfermedadAutoinmune() != null) antecedentes.setEnfermedadAutoinmune(request.enfermedadAutoinmune());
        if (request.anemiaPrevia() != null) antecedentes.setAnemiaPrevia(request.anemiaPrevia());
        if (request.vihPositivo() != null) antecedentes.setVihPositivo(request.vihPositivo());
        if (request.sifilisPrevia() != null) antecedentes.setSifilisPrevia(request.sifilisPrevia());
        if (request.trastornoCoagulacion() != null) antecedentes.setTrastornoCoagulacion(request.trastornoCoagulacion());
        if (request.residenciaAltitud() != null) {
            antecedentes.setResidenciaAltitud(request.residenciaAltitud());
        } else {
            // Predeterminado: Inferir residencia por altitud si la paciente es de Huánuco (sobre los 2500msnm)
            // Para simplificar, en esta etapa lo dejamos en falso a menos que sea especificado.
            antecedentes.setResidenciaAltitud(false);
        }
        if (request.obesidadPregestacional() != null) antecedentes.setObesidadPregestacional(request.obesidadPregestacional());
        if (request.observaciones() != null) antecedentes.setObservaciones(request.observaciones());

        antecedentes = antecedentesRepository.save(antecedentes);
        return mapToAntecedentesResponse(antecedentes);
    }

    @Transactional
    public EmbarazoResponse finalizarEmbarazo(UUID id, FinalizarEmbarazoRequest request) {
        Embarazo embarazo = embarazoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", id.toString()));

        if (request.estado() == EstadoEmbarazo.ACTIVO) {
            throw new BusinessRuleException("INVALID_STATE", "No se puede finalizar un embarazo al estado ACTIVO");
        }

        embarazo.setEstado(request.estado());
        embarazo.setFechaFin(request.fechaFin());
        embarazo = embarazoRepository.save(embarazo);

        return mapToEmbarazoResponse(embarazo);
    }

    public AntecedentesResponse obtenerAntecedentes(UUID embarazoId) {
        AntecedentesClinicos antecedentes = antecedentesRepository.findById(embarazoId)
                .orElseThrow(() -> new ResourceNotFoundException("Antecedentes clínicos", embarazoId.toString()));
        return mapToAntecedentesResponse(antecedentes);
    }

    public Embarazo getEmbarazoEntityById(UUID id) {
        return embarazoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", id.toString()));
    }

    private EmbarazoResponse mapToEmbarazoResponse(Embarazo embarazo) {
        LocalDate fin = embarazo.getEstado() == EstadoEmbarazo.ACTIVO ? LocalDate.now() : embarazo.getFechaFin();
        long semanasActuales = ChronoUnit.WEEKS.between(embarazo.getFechaUltimaMenstruacion(), fin);
        
        int trimestre = 1;
        if (semanasActuales > 26) {
            trimestre = 3;
        } else if (semanasActuales > 13) {
            trimestre = 2;
        }

        return new EmbarazoResponse(
                embarazo.getId(),
                embarazo.getPacienteId(),
                embarazo.getMedicoId(),
                embarazo.getFechaUltimaMenstruacion(),
                embarazo.getFechaProbableParto(),
                embarazo.getFechaProbablePartoEco(),
                embarazo.getSemanasGestacionIngreso(),
                semanasActuales,
                trimestre,
                embarazo.getNumeroGestacion(),
                embarazo.getNumeroPartos(),
                embarazo.getNumeroAbortos(),
                embarazo.getNumeroCesareas(),
                embarazo.isEmbarazoMultiple(),
                embarazo.getEstado(),
                embarazo.getFechaFin()
        );
    }

    private AntecedentesResponse mapToAntecedentesResponse(AntecedentesClinicos ant) {
        return new AntecedentesResponse(
                ant.getId(),
                ant.isDiabetesPrevia(),
                ant.isHipertensionPrevia(),
                ant.isPreeclampsiaPrevia(),
                ant.isEnfermedadRenal(),
                ant.isEnfermedadAutoinmune(),
                ant.isAnemiaPrevia(),
                ant.isVihPositivo(),
                ant.isSifilisPrevia(),
                ant.isTrastornoCoagulacion(),
                ant.isEdadMaternaRiesgo(),
                ant.isResidenciaAltitud(),
                ant.isObesidadPregestacional(),
                ant.getObservaciones()
        );
    }
}
