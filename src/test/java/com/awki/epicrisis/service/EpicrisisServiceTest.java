package com.awki.epicrisis.service;

import com.awki.alerta.repository.AlertaRepository;
import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.auth.entity.Usuario;
import com.awki.auth.repository.MedicoRepository;
import com.awki.auth.repository.PacienteRepository;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.chat.repository.ResumenClinicoRepository;
import com.awki.embarazo.entity.AntecedentesClinicos;
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
import com.awki.exception.TenantViolationException;
import com.awki.chat.service.GeminiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpicrisisServiceTest {

    @Mock
    private EpicrisisRepository epicrisisRepository;
    @Mock
    private EpicrisisJobRepository jobRepository;
    @Mock
    private EmbarazoRepository embarazoRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private EpicrisisAsyncService asyncService;
    @Mock
    private DocumentStorageService storageService;

    // Dependencias para EpicrisisAsyncService
    @Mock
    private AlertaRepository alertaRepository;
    @Mock
    private ResumenClinicoRepository resumenClinicoRepository;
    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private EpicrisisService epicrisisService;

    private EpicrisisAsyncService testAsyncService;

    private UUID medicoUserId;
    private UUID medicoId;
    private UUID pacienteId;
    private UUID embarazoId;
    private UUID clinicaId;
    
    private Medico medico;
    private Paciente paciente;
    private Embarazo embarazo;
    private Usuario medicoUsuario;
    private Usuario pacienteUsuario;
    private UsuarioAutenticado authUser;

    @BeforeEach
    void setUp() {
        medicoUserId = UUID.randomUUID();
        medicoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        embarazoId = UUID.randomUUID();
        clinicaId = UUID.randomUUID();

        medicoUsuario = new Usuario();
        medicoUsuario.setClinicaId(clinicaId);
        medico = new Medico();
        ReflectionTestUtils.setField(medico, "id", medicoId);
        medico.setUsuario(medicoUsuario);

        pacienteUsuario = new Usuario();
        pacienteUsuario.setClinicaId(clinicaId);
        paciente = new Paciente();
        ReflectionTestUtils.setField(paciente, "id", pacienteId);
        paciente.setNombres("Maria");
        paciente.setApellidos("Gomez");
        paciente.setUsuario(pacienteUsuario);

        embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);
        embarazo.setPacienteId(pacienteId);
        embarazo.setFechaUltimaMenstruacion(LocalDate.now().minusWeeks(35));

        AntecedentesClinicos antecedentes = new AntecedentesClinicos();
        antecedentes.setEmbarazo(embarazo);
        embarazo.setAntecedentes(antecedentes);

        authUser = new UsuarioAutenticado(medicoUserId, "MEDICO", clinicaId);

        // Instanciar EpicrisisAsyncService manualmente para probar su lógica
        testAsyncService = new EpicrisisAsyncService(
                jobRepository,
                epicrisisRepository,
                embarazoRepository,
                pacienteRepository,
                alertaRepository,
                resumenClinicoRepository,
                geminiClient,
                storageService
        );
    }

    @Test
    void testIniciarGeneracionExito() {
        EpicrisisGenerarRequest req = new EpicrisisGenerarRequest(embarazoId, "Derivación por parto", "Ninguna");

        when(medicoRepository.findByUsuario_Id(medicoUserId)).thenReturn(Optional.of(medico));
        when(embarazoRepository.findById(embarazoId)).thenReturn(Optional.of(embarazo));
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));

        EpicrisisJob mockJob = new EpicrisisJob();
        ReflectionTestUtils.setField(mockJob, "id", UUID.randomUUID());
        mockJob.setEmbarazoId(embarazoId);
        mockJob.setMedicoId(medicoId);
        mockJob.setClinicaId(clinicaId);
        mockJob.setEstado(EstadoJob.PROCESANDO);

        when(jobRepository.save(any(EpicrisisJob.class))).thenReturn(mockJob);

        EpicrisisJobResponse response = epicrisisService.iniciarGeneracion(req, authUser);

        assertNotNull(response);
        assertEquals("PROCESANDO", response.estado());
        verify(asyncService, times(1)).generarEpicrisisAsync(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testIniciarGeneracionEmbarazoAjenoLanzaException() {
        EpicrisisGenerarRequest req = new EpicrisisGenerarRequest(embarazoId, "Derivación por parto", "Ninguna");

        // Cambiar clínica del paciente a una diferente
        pacienteUsuario.setClinicaId(UUID.randomUUID());

        when(medicoRepository.findByUsuario_Id(medicoUserId)).thenReturn(Optional.of(medico));
        when(embarazoRepository.findById(embarazoId)).thenReturn(Optional.of(embarazo));
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));

        assertThrows(TenantViolationException.class, () -> epicrisisService.iniciarGeneracion(req, authUser));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void testObtenerEstadoJobAjenoLanzaException() {
        UUID jobId = UUID.randomUUID();
        EpicrisisJob job = new EpicrisisJob();
        job.setClinicaId(UUID.randomUUID()); // Clínica diferente

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(medicoRepository.findByUsuario_Id(medicoUserId)).thenReturn(Optional.of(medico));

        assertThrows(TenantViolationException.class, () -> epicrisisService.obtenerEstadoJob(jobId, authUser));
    }

    @Test
    void testDescargarPdfAjenoLanzaException() {
        UUID epicrisisId = UUID.randomUUID();
        Epicrisis epicrisis = new Epicrisis();
        epicrisis.setClinicaId(UUID.randomUUID()); // Clínica diferente

        when(epicrisisRepository.findById(epicrisisId)).thenReturn(Optional.of(epicrisis));
        when(medicoRepository.findByUsuario_Id(medicoUserId)).thenReturn(Optional.of(medico));

        assertThrows(TenantViolationException.class, () -> epicrisisService.descargarPdf(epicrisisId, authUser));
    }

    @Test
    void testFallbackLocalEpicrisisAsyncService() {
        UUID jobId = UUID.randomUUID();
        EpicrisisJob job = new EpicrisisJob();
        ReflectionTestUtils.setField(job, "id", jobId);
        job.setEstado(EstadoJob.PROCESANDO);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embarazoRepository.findById(embarazoId)).thenReturn(Optional.of(embarazo));
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(alertaRepository.findByEmbarazo_Id(embarazoId)).thenReturn(new ArrayList<>());
        when(resumenClinicoRepository.findById(embarazoId)).thenReturn(Optional.empty());

        // Simular falla de Gemini API
        when(geminiClient.generarContenido(any(), anyDouble(), anyInt())).thenReturn(Optional.empty());

        // Mock guardar documento
        when(storageService.guardarDocumento(any(), any())).thenReturn("mock_file.pdf");

        // Mock guardar epicrisis
        Epicrisis mockEpicrisis = new Epicrisis();
        ReflectionTestUtils.setField(mockEpicrisis, "id", UUID.randomUUID());
        when(epicrisisRepository.save(any())).thenReturn(mockEpicrisis);

        // Ejecutar método síncronamente para probar
        testAsyncService.generarEpicrisisAsync(jobId, embarazoId, medicoId, clinicaId, "Derivacion", "Obs");

        assertEquals(EstadoJob.COMPLETADO, job.getEstado());
        assertNotNull(job.getEpicrisis());
        verify(epicrisisRepository, times(1)).save(any(Epicrisis.class));
    }
}
