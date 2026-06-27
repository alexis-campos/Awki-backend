package com.awki.alerta.service;

import com.awki.alerta.dto.*;
import com.awki.alerta.entity.Alerta;
import com.awki.alerta.entity.DispositivoMedico;
import com.awki.alerta.entity.EstadoEntrega;
import com.awki.alerta.entity.NivelUrgencia;
import com.awki.alerta.entity.OrigenAlerta;
import com.awki.alerta.entity.TipoAlerta;
import com.awki.alerta.repository.AlertaRepository;
import com.awki.alerta.repository.ContactoEmergenciaRepository;
import com.awki.alerta.repository.DispositivoMedicoRepository;
import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.auth.service.AuthService;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.clinica.entity.Clinica;
import com.awki.clinica.service.ClinicaService;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.service.EmbarazoService;
import com.awki.exception.ResourceNotFoundException;
import com.awki.exception.TenantViolationException;
import com.awki.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final ContactoEmergenciaRepository contactoRepository;
    private final DispositivoMedicoRepository dispositivoMedicoRepository;
    private final AuthService authService;
    private final EmbarazoService embarazoService;
    private final ClinicaService clinicaService;
    private final NotificationAsyncService notificationAsyncService;

    @Transactional
    public SosResponse crearSos(SosRequest request, UsuarioAutenticado user) {
        log.info("Creando SOS manual para el embarazo: {} por el usuario: {}", request.embarazoId(), user.id());

        Paciente paciente = authService.getPacienteEntityByUsuarioId(user.id());

        Embarazo embarazo = embarazoService.getEmbarazoEntityById(request.embarazoId());

        if (!embarazo.getPacienteId().equals(paciente.getId())) {
            throw new TenantViolationException("El embarazo no pertenece a la paciente autenticada");
        }

        Medico medico = null;
        if (embarazo.getMedicoId() != null) {
            medico = authService.getMedicoEntityById(embarazo.getMedicoId()).orElse(null);
        }

        Clinica clinica = resolverClinica(paciente, medico);

        Alerta alerta = new Alerta();
        alerta.setEmbarazo(embarazo);
        alerta.setPaciente(paciente);
        alerta.setMedico(medico);
        alerta.setClinica(clinica);
        alerta.setTipoAlerta(TipoAlerta.SOS_MANUAL);
        alerta.setNivelUrgencia(NivelUrgencia.ROJO);
        alerta.setOrigen(OrigenAlerta.SOS);

        String desc = "Botón de pánico SOS activado por la paciente";
        if (request.mensajeLibre() != null && !request.mensajeLibre().isBlank()) {
            desc = request.mensajeLibre();
        }
        alerta.setDescripcion(desc);
        alerta.setLatitud(request.latitud());
        alerta.setLongitud(request.longitud());
        alerta.setMensajeLibre(request.mensajeLibre());
        alerta.setEstadoEntrega(EstadoEntrega.PENDIENTE);

        Alerta guardada = alertaRepository.save(alerta);

        notificarAlertaDespuesCommit(guardada.getId());

        int contactosNotificados = contactoRepository.findByPaciente_IdAndActivoTrue(paciente.getId()).size();
        boolean medicoNotificado = (medico != null);

        return new SosResponse(
                guardada.getId(),
                NivelUrgencia.ROJO.name(),
                contactosNotificados,
                medicoNotificado,
                LocalDateTime.now()
        );
    }

    @Transactional
    public void crearAlertaDesdeRiesgo(CrearAlertaInternaRequest request) {
        log.info("Creando alerta interna desde riesgo/chat. Embarazo: {} - Nivel: {}", request.embarazoId(), request.nivelUrgencia());

        Embarazo embarazo = embarazoService.getEmbarazoEntityById(request.embarazoId());

        Paciente paciente = authService.getPacienteEntityById(embarazo.getPacienteId());

        Medico medico = null;
        if (embarazo.getMedicoId() != null) {
            medico = authService.getMedicoEntityById(embarazo.getMedicoId()).orElse(null);
        }

        Clinica clinica = resolverClinica(paciente, medico);

        Alerta alerta = new Alerta();
        alerta.setEmbarazo(embarazo);
        alerta.setPaciente(paciente);
        alerta.setMedico(medico);
        alerta.setClinica(clinica);
        alerta.setTipoAlerta(request.tipoAlerta());
        alerta.setNivelUrgencia(request.nivelUrgencia());
        alerta.setDescripcion(request.descripcion());
        alerta.setSintomasDisparadores(request.sintomasDisparadores());
        alerta.setOrigen(request.origen());
        alerta.setEstadoEntrega(EstadoEntrega.PENDIENTE);

        Alerta guardada = alertaRepository.save(alerta);

        notificarAlertaDespuesCommit(guardada.getId());
    }

    @Transactional(readOnly = true)
    public Page<AlertaResponse> listarAlertas(UsuarioAutenticado user, Pageable pageable) {
        if ("MEDICO".equalsIgnoreCase(user.rol())) {
            Medico medico = authService.getMedicoEntityByUsuarioId(user.id());

            Page<Alerta> alertas = alertaRepository.findByMedico_IdOrderByNivelUrgenciaDescCreatedAtDesc(medico.getId(), pageable);
            return alertas.map(this::toResponse);
        } else if ("ADMIN_CLINICA".equalsIgnoreCase(user.rol())) {
            UUID clinicaId = user.clinicaId();
            if (clinicaId == null) {
                throw new TenantViolationException("El administrador no tiene una clínica asociada");
            }
            Page<Alerta> alertas = alertaRepository.findByClinica_IdOrderByNivelUrgenciaDescCreatedAtDesc(clinicaId, pageable);
            return alertas.map(this::toResponse);
        } else {
            throw new TenantViolationException("Rol no autorizado para listar alertas");
        }
    }

    @Transactional(readOnly = true)
    public CountNoLeidasResponse contarNoLeidas(UsuarioAutenticado user) {
        if (!"MEDICO".equalsIgnoreCase(user.rol())) {
            throw new TenantViolationException("Rol no autorizado para contar alertas");
        }
        Medico medico = authService.getMedicoEntityByUsuarioId(user.id());

        long count = alertaRepository.countByMedico_IdAndVistaPorMedicoFalse(medico.getId());
        return new CountNoLeidasResponse(count);
    }

    @Transactional
    public void marcarLeida(UUID alertaId, UsuarioAutenticado user) {
        if (!"MEDICO".equalsIgnoreCase(user.rol())) {
            throw new TenantViolationException("Rol no autorizado para marcar alerta como leída");
        }

        Medico medico = authService.getMedicoEntityByUsuarioId(user.id());

        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta", alertaId.toString()));

        if (alerta.getMedico() == null || !alerta.getMedico().getId().equals(medico.getId())) {
            throw new TenantViolationException("La alerta no está asignada al médico autenticado");
        }

        alerta.setVistaPorMedico(true);
        alerta.setFechaVista(LocalDateTime.now());
        alertaRepository.save(alerta);
    }

    @Transactional
    public void registrarFcmToken(RegistrarTokenRequest request, UsuarioAutenticado user) {
        if (!"MEDICO".equalsIgnoreCase(user.rol())) {
            throw new TenantViolationException("Solo los médicos pueden registrar tokens FCM");
        }

        Medico medico = authService.getMedicoEntityByUsuarioId(user.id());

        String token = request.fcmToken();

        dispositivoMedicoRepository.findByMedico_IdAndFcmToken(medico.getId(), token)
                .orElseGet(() -> {
                    DispositivoMedico dm = new DispositivoMedico();
                    dm.setMedico(medico);
                    dm.setFcmToken(token);
                    return dispositivoMedicoRepository.save(dm);
                });
    }

    @Transactional
    public void crearAlertaRetroactivaDesdeSync(CrearAlertaInternaRequest request, LocalDateTime fechaRetroactiva) {
        log.info("[AlertaService] Creando alerta retroactiva. Embarazo: {} - Nivel: {}", request.embarazoId(), request.nivelUrgencia());

        Embarazo embarazo = embarazoService.getEmbarazoEntityById(request.embarazoId());

        Paciente paciente = authService.getPacienteEntityById(embarazo.getPacienteId());

        Medico medico = null;
        if (embarazo.getMedicoId() != null) {
            medico = authService.getMedicoEntityById(embarazo.getMedicoId()).orElse(null);
        }

        Clinica clinica = resolverClinica(paciente, medico);

        Alerta alerta = new Alerta();
        alerta.setEmbarazo(embarazo);
        alerta.setPaciente(paciente);
        alerta.setMedico(medico);
        alerta.setClinica(clinica);
        alerta.setTipoAlerta(request.tipoAlerta());
        alerta.setNivelUrgencia(request.nivelUrgencia());
        alerta.setDescripcion(request.descripcion());
        alerta.setSintomasDisparadores(request.sintomasDisparadores());
        alerta.setOrigen(request.origen());
        alerta.setEstadoEntrega(EstadoEntrega.PENDIENTE);
        alerta.setAlertaRetroactiva(true);
        alerta.setFechaRetroactiva(fechaRetroactiva);

        alertaRepository.save(alerta);
    }

    @Transactional(readOnly = true)
    public String getAlertasFormatadasParaEpicrisis(UUID embarazoId) {
        List<Alerta> alertas = alertaRepository.findByEmbarazo_Id(embarazoId);
        return alertas.isEmpty() ? "Sin alertas clínicas registradas." :
                alertas.stream()
                        .map(a -> "- " + a.getTipoAlerta() + " (" + a.getNivelUrgencia() + "): " + a.getDescripcion())
                        .collect(Collectors.joining("\n"));
    }

    private AlertaResponse toResponse(Alerta alerta) {
        String nombrePaciente = "Sistema";
        if (alerta.getPaciente() != null) {
            nombrePaciente = alerta.getPaciente().getNombres() + " " + alerta.getPaciente().getApellidos();
        }

        return new AlertaResponse(
                alerta.getId(),
                alerta.getEmbarazo().getId(),
                alerta.getPaciente() != null ? alerta.getPaciente().getId() : null,
                nombrePaciente,
                alerta.getMedico() != null ? alerta.getMedico().getId() : null,
                alerta.getClinica().getId(),
                alerta.getTipoAlerta().name(),
                alerta.getNivelUrgencia().name(),
                alerta.getDescripcion(),
                alerta.getSintomasDisparadores(),
                alerta.getOrigen().name(),
                alerta.isVistaPorMedico(),
                alerta.getFechaVista(),
                alerta.getEstadoEntrega().name(),
                alerta.isWebsocketEnviado(),
                alerta.isSmsEnviado(),
                alerta.isWhatsappEnviado(),
                alerta.isFcmEnviado(),
                alerta.getLatitud(),
                alerta.getLongitud(),
                alerta.getMensajeLibre(),
                alerta.getCreatedAt()
        );
    }

    private Clinica resolverClinica(Paciente paciente, Medico medico) {
        UUID rawId = paciente.getUsuario().getClinicaId();
        if (rawId == null && medico != null) {
            rawId = medico.getClinicaId();
        }
        if (rawId == null) {
            throw new ResourceNotFoundException("Clinica", "no se encontró clínica asociada a la paciente ni al médico");
        }
        return clinicaService.getClinicaEntityById(rawId);
    }

    private void notificarAlertaDespuesCommit(UUID alertaId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationAsyncService.notificarAlertaAsync(alertaId);
                }
            });
        } else {
            notificationAsyncService.notificarAlertaAsync(alertaId);
        }
    }
}
