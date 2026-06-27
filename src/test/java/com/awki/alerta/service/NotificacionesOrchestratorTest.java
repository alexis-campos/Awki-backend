package com.awki.alerta.service;

import com.awki.alerta.client.PushNotificationClient;
import com.awki.alerta.client.SmsNotificationClient;
import com.awki.alerta.client.WhatsappNotificationClient;
import com.awki.alerta.entity.*;
import com.awki.alerta.repository.AlertaRepository;
import com.awki.alerta.repository.ContactoEmergenciaRepository;
import com.awki.alerta.repository.DispositivoMedicoRepository;
import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.clinica.entity.Clinica;
import com.awki.embarazo.entity.Embarazo;
import com.awki.websocket.dto.AlertaPayload;
import com.awki.websocket.service.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionesOrchestratorTest {

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private ContactoEmergenciaRepository contactoRepository;

    @Mock
    private DispositivoMedicoRepository dispositivoMedicoRepository;

    @Mock
    private SmsNotificationClient smsClient;

    @Mock
    private WhatsappNotificationClient whatsappClient;

    @Mock
    private PushNotificationClient pushClient;

    @Mock
    private WebSocketNotificationService webSocketService;

    @InjectMocks
    private NotificacionesOrchestrator orchestrator;

    private UUID alertaId;
    private Alerta alerta;
    private Medico medico;
    private Paciente paciente;
    private Embarazo embarazo;
    private Clinica clinica;

    @BeforeEach
    void setUp() {
        alertaId = UUID.randomUUID();
        
        paciente = new Paciente();
        ReflectionTestUtils.setField(paciente, "id", UUID.randomUUID());
        paciente.setNombres("Maria");
        paciente.setApellidos("Lopez");

        medico = new Medico();
        ReflectionTestUtils.setField(medico, "id", UUID.randomUUID());

        embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", UUID.randomUUID());

        clinica = new Clinica();

        alerta = new Alerta();
        ReflectionTestUtils.setField(alerta, "id", alertaId);
        alerta.setEmbarazo(embarazo);
        alerta.setPaciente(paciente);
        alerta.setMedico(medico);
        alerta.setClinica(clinica);
        alerta.setTipoAlerta(TipoAlerta.SINTOMA_CRITICO_IA);
        alerta.setNivelUrgencia(NivelUrgencia.ROJO);
        alerta.setOrigen(OrigenAlerta.CHAT);
        alerta.setDescripcion("Síntoma crítico");
    }

    @Test
    void testOrquestarWebSocketExito() {
        when(alertaRepository.findById(alertaId)).thenReturn(Optional.of(alerta));
        doNothing().when(webSocketService).enviarAlertaNueva(any(UUID.class), any(AlertaPayload.class));

        orchestrator.orquestarNotificaciones(alertaId);

        assertTrue(alerta.isWebsocketEnviado());
        assertEquals(EstadoEntrega.ENTREGADA_WEBSOCKET, alerta.getEstadoEntrega());
        verify(smsClient, never()).enviarSms(anyString(), anyString());
        verify(alertaRepository, times(1)).save(alerta);
    }

    @Test
    void testOrquestarWebSocketFallaIniciaSmsYFcm() {
        when(alertaRepository.findById(alertaId)).thenReturn(Optional.of(alerta));
        
        // Simular que falla el WebSocket
        doThrow(new RuntimeException("WebSocket desconectado"))
                .when(webSocketService).enviarAlertaNueva(any(UUID.class), any(AlertaPayload.class));
        
        when(smsClient.enviarSms(anyString(), anyString())).thenReturn(true);
        when(dispositivoMedicoRepository.findByMedico_Id(any(UUID.class))).thenReturn(new ArrayList<>());

        orchestrator.orquestarNotificaciones(alertaId);

        assertFalse(alerta.isWebsocketEnviado());
        assertTrue(alerta.isSmsEnviado());
        assertEquals(EstadoEntrega.ENTREGADA_SMS, alerta.getEstadoEntrega());
        verify(smsClient, times(1)).enviarSms(anyString(), anyString());
        verify(alertaRepository, times(1)).save(alerta);
    }
}
