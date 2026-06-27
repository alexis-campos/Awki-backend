package com.awki.epicrisis.service;

import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.auth.repository.MedicoRepository;
import com.awki.auth.repository.PacienteRepository;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.repository.EmbarazoRepository;
import com.awki.epicrisis.dto.EpicrisisGenerarRequest;
import com.awki.epicrisis.dto.EpicrisisJobResponse;
import com.awki.epicrisis.dto.EpicrisisResponse;
import com.awki.epicrisis.entity.Epicrisis;
import com.awki.epicrisis.entity.EpicrisisJob;
import com.awki.epicrisis.entity.EstadoJob;
import com.awki.epicrisis.repository.EpicrisisJobRepository;
import com.awki.epicrisis.repository.EpicrisisRepository;
import com.awki.exception.ResourceNotFoundException;
import com.awki.exception.TenantViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpicrisisService {

    private final EpicrisisRepository epicrisisRepository;
    private final EpicrisisJobRepository jobRepository;
    private final EmbarazoRepository embarazoRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final EpicrisisAsyncService asyncService;
    private final DocumentStorageService storageService;

    public EpicrisisJobResponse iniciarGeneracion(EpicrisisGenerarRequest request, UsuarioAutenticado user) {
        log.info("Solicitud de generación de epicrisis recibida del médico: {} para embarazo: {}", user.id(), request.embarazoId());

        // 1. Obtener Médico y su Clínica
        Medico medico = medicoRepository.findByUsuario_Id(user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Medico", user.id().toString()));
        UUID docClinicaId = medico.getUsuario().getClinicaId();

        // 2. Obtener Embarazo y validar Clínica
        Embarazo embarazo = embarazoRepository.findById(request.embarazoId())
                .orElseThrow(() -> new ResourceNotFoundException("Embarazo", request.embarazoId().toString()));

        Paciente paciente = pacienteRepository.findById(embarazo.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", embarazo.getPacienteId().toString()));

        UUID pacienteClinicaId = paciente.getUsuario().getClinicaId();

        boolean tieneAcceso;
        if (pacienteClinicaId != null) {
            tieneAcceso = pacienteClinicaId.equals(docClinicaId);
        } else {
            // Paciente standalone: solo el médico vinculado al embarazo puede generar epicrisis
            tieneAcceso = medico.getId().equals(embarazo.getMedicoId());
        }

        if (!tieneAcceso) {
            log.warn("Violación de tenencia: Médico {} (Clínica {}) intentó generar epicrisis para Paciente {} (Clínica {})",
                    medico.getId(), docClinicaId, paciente.getId(), pacienteClinicaId);
            throw new TenantViolationException("El embarazo no pertenece a la misma clínica que el médico");
        }

        // 3. Persistir el Job en su propia transacción para garantizar commit antes del async
        EpicrisisJob guardado = crearJobTransaccional(request.embarazoId(), medico.getId(), docClinicaId);

        // 4. Iniciar procesamiento asíncrono tras el commit
        asyncService.generarEpicrisisAsync(
                guardado.getId(),
                request.embarazoId(),
                medico.getId(),
                docClinicaId,
                request.motivoDerivacion(),
                request.observacionesAdicionales()
        );

        return toJobResponse(guardado);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public EpicrisisJob crearJobTransaccional(UUID embarazoId, UUID medicoId, UUID clinicaId) {
        EpicrisisJob job = new EpicrisisJob();
        job.setEmbarazoId(embarazoId);
        job.setMedicoId(medicoId);
        job.setClinicaId(clinicaId);
        job.setEstado(EstadoJob.PROCESANDO);
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public EpicrisisJobResponse obtenerEstadoJob(UUID jobId, UsuarioAutenticado user) {
        EpicrisisJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("EpicrisisJob", jobId.toString()));

        // Obtener clínica del médico autenticado
        Medico medico = medicoRepository.findByUsuario_Id(user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Medico", user.id().toString()));
        UUID docClinicaId = medico.getUsuario().getClinicaId();

        if (docClinicaId == null || !docClinicaId.equals(job.getClinicaId())) {
            log.warn("Violación de tenencia en consulta de estado: Médico {} (Clínica {}) intentó ver Job {} (Clínica {})",
                    medico.getId(), docClinicaId, jobId, job.getClinicaId());
            throw new TenantViolationException("El job de epicrisis solicitado no pertenece a su clínica");
        }

        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public EpicrisisResponse obtenerEpicrisis(UUID epicrisisId, UsuarioAutenticado user) {
        Epicrisis epicrisis = epicrisisRepository.findById(epicrisisId)
                .orElseThrow(() -> new ResourceNotFoundException("Epicrisis", epicrisisId.toString()));

        // Obtener clínica del médico autenticado
        Medico medico = medicoRepository.findByUsuario_Id(user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Medico", user.id().toString()));
        UUID docClinicaId = medico.getUsuario().getClinicaId();

        if (docClinicaId == null || !docClinicaId.equals(epicrisis.getClinicaId())) {
            log.warn("Violación de tenencia al ver contenido: Médico {} (Clínica {}) intentó ver Epicrisis {} (Clínica {})",
                    medico.getId(), docClinicaId, epicrisisId, epicrisis.getClinicaId());
            throw new TenantViolationException("La epicrisis solicitada no pertenece a su clínica");
        }

        return toEpicrisisResponse(epicrisis);
    }

    @Transactional(readOnly = true)
    public byte[] descargarPdf(UUID epicrisisId, UsuarioAutenticado user) {
        Epicrisis epicrisis = epicrisisRepository.findById(epicrisisId)
                .orElseThrow(() -> new ResourceNotFoundException("Epicrisis", epicrisisId.toString()));

        // Obtener clínica del médico autenticado
        Medico medico = medicoRepository.findByUsuario_Id(user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Medico", user.id().toString()));
        UUID docClinicaId = medico.getUsuario().getClinicaId();

        if (docClinicaId == null || !docClinicaId.equals(epicrisis.getClinicaId())) {
            log.warn("Violación de tenencia al descargar PDF: Médico {} (Clínica {}) intentó descargar Epicrisis {} (Clínica {})",
                    medico.getId(), docClinicaId, epicrisisId, epicrisis.getClinicaId());
            throw new TenantViolationException("La epicrisis solicitada no pertenece a su clínica");
        }

        return storageService.obtenerDocumento(epicrisis.getUrlPdf());
    }

    private EpicrisisJobResponse toJobResponse(EpicrisisJob j) {
        return new EpicrisisJobResponse(
                j.getId(),
                j.getEmbarazoId(),
                j.getMedicoId(),
                j.getClinicaId(),
                j.getEstado().name(),
                j.getEpicrisis() != null ? j.getEpicrisis().getId() : null,
                j.getMensajeError(),
                j.getEpicrisis() != null ? j.getEpicrisis().getUrlPdf() : null,
                j.getCreatedAt()
        );
    }

    private EpicrisisResponse toEpicrisisResponse(Epicrisis e) {
        return new EpicrisisResponse(
                e.getId(),
                e.getEmbarazoId(),
                e.getMedicoId(),
                e.getClinicaId(),
                e.getMotivoDerivacion(),
                e.getObservacionesAdicionales(),
                e.getContenidoJson(),
                e.getUrlPdf(),
                e.getCreatedAt()
        );
    }
}
