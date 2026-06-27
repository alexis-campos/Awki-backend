package com.awki.alerta.service;

import com.awki.alerta.dto.SosRequest;
import com.awki.alerta.dto.SosResponse;
import com.awki.alerta.entity.Alerta;
import com.awki.alerta.entity.NivelUrgencia;
import com.awki.alerta.entity.OrigenAlerta;
import com.awki.alerta.entity.TipoAlerta;
import com.awki.alerta.repository.AlertaRepository;
import com.awki.alerta.repository.ContactoEmergenciaRepository;
import com.awki.alerta.repository.DispositivoMedicoRepository;
import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.auth.entity.Usuario;
import com.awki.auth.repository.MedicoRepository;
import com.awki.auth.repository.PacienteRepository;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.clinica.entity.Clinica;
import com.awki.clinica.repository.ClinicaRepository;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.repository.EmbarazoRepository;
import com.awki.exception.TenantViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private ContactoEmergenciaRepository contactoRepository;

    @Mock
    private DispositivoMedicoRepository dispositivoMedicoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private EmbarazoRepository embarazoRepository;

    @Mock
    private ClinicaRepository clinicaRepository;

    @Mock
    private NotificationAsyncService notificationAsyncService;

    @InjectMocks
    private AlertaService alertaService;

    private UUID userId;
    private UUID embarazoId;
    private UUID pacienteId;
    private UUID clinicaId;
    private Paciente paciente;
    private Embarazo embarazo;
    private Clinica clinica;
    private Usuario usuario;
    private UsuarioAutenticado authUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        embarazoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        clinicaId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setClinicaId(clinicaId);

        paciente = new Paciente();
        ReflectionTestUtils.setField(paciente, "id", pacienteId);
        paciente.setUsuario(usuario);

        embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);
        embarazo.setPacienteId(pacienteId);

        clinica = new Clinica();

        authUser = new UsuarioAutenticado(userId, "PACIENTE", clinicaId);
    }

    @Test
    void testCrearSosExito() {
        SosRequest req = new SosRequest(embarazoId, new BigDecimal("-12.046"), new BigDecimal("-77.042"), "Mensaje de pánico");

        when(pacienteRepository.findByUsuario_Id(userId)).thenReturn(Optional.of(paciente));
        when(embarazoRepository.findById(embarazoId)).thenReturn(Optional.of(embarazo));
        when(clinicaRepository.findById(clinicaId)).thenReturn(Optional.of(clinica));
        when(contactoRepository.findByPaciente_IdAndActivoTrue(pacienteId)).thenReturn(new ArrayList<>());

        Alerta savedAlerta = new Alerta();
        ReflectionTestUtils.setField(savedAlerta, "id", UUID.randomUUID());
        savedAlerta.setTipoAlerta(TipoAlerta.SOS_MANUAL);
        savedAlerta.setNivelUrgencia(NivelUrgencia.ROJO);
        savedAlerta.setOrigen(OrigenAlerta.SOS);
        
        when(alertaRepository.save(any(Alerta.class))).thenReturn(savedAlerta);

        SosResponse resp = alertaService.crearSos(req, authUser);

        assertNotNull(resp);
        assertEquals(NivelUrgencia.ROJO.name(), resp.nivel());
        assertEquals(0, resp.contactosNotificados());
        assertFalse(resp.medicoNotificado());
        verify(notificationAsyncService, times(1)).notificarAlertaAsync(any(UUID.class));
    }

    @Test
    void testCrearSosEmbarazoAjenoLanzaException() {
        SosRequest req = new SosRequest(embarazoId, null, null, null);
        
        UUID otroPacienteId = UUID.randomUUID();
        embarazo.setPacienteId(otroPacienteId); // Cambiar paciente dueño

        when(pacienteRepository.findByUsuario_Id(userId)).thenReturn(Optional.of(paciente));
        when(embarazoRepository.findById(embarazoId)).thenReturn(Optional.of(embarazo));

        assertThrows(TenantViolationException.class, () -> alertaService.crearSos(req, authUser));
        verify(alertaRepository, never()).save(any(Alerta.class));
    }
}
