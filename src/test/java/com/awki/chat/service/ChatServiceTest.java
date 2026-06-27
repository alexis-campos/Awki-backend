package com.awki.chat.service;

import com.awki.auth.service.AuthService;
import com.awki.chat.dto.*;
import com.awki.chat.entity.MensajeChat;
import com.awki.chat.entity.ResumenClinico;
import com.awki.chat.repository.MensajeChatRepository;
import com.awki.chat.repository.ResumenClinicoRepository;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.repository.EmbarazoRepository;
import com.awki.exception.BusinessRuleException;
import com.awki.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MensajeChatRepository mensajeChatRepository;

    @Mock
    private ResumenClinicoRepository resumenClinicoRepository;

    @Mock
    private EmbarazoRepository embarazoRepository;

    @Mock
    private AuthService authService;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private SemanticCacheService semanticCacheService;

    @Mock
    private ChatAsyncService chatAsyncService;

    @InjectMocks
    private ChatService chatService;

    private UUID pregnancyId;
    private UUID patientId;
    private UUID clinicaId;
    private Embarazo embarazo;

    @BeforeEach
    void setUp() {
        pregnancyId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        clinicaId = UUID.randomUUID();

        embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", pregnancyId);
        embarazo.setPacienteId(patientId);
        embarazo.setFechaUltimaMenstruacion(LocalDate.now().minusWeeks(12));
        embarazo.setEmbarazoMultiple(false);
        embarazo.setNumeroGestacion(1);
        embarazo.setNumeroPartos(0);
        embarazo.setNumeroCesareas(0);

        lenient().when(semanticCacheService.normalizar(anyString())).thenAnswer(invocation -> {
            String val = invocation.getArgument(0);
            return val != null ? val.toLowerCase().trim() : "";
        });
    }

    @Test
    void detectarAlarmaProbable_True_WhenSevereSymptomsPresent() {
        assertTrue(chatService.detectarAlarmaProbable("Tengo un sangrado muy fuerte"));
        assertTrue(chatService.detectarAlarmaProbable("Siento una cefalea intensa en la cabeza"));
        assertTrue(chatService.detectarAlarmaProbable("Perdida de liquido amniotico"));
        assertFalse(chatService.detectarAlarmaProbable("¿Puedo comer manzanas en el embarazo?"));
    }

    @Test
    void enviarMensaje_MarksAlarmaProbable_WhenSymptomIsDetected() {
        ChatMensajeRequest request = new ChatMensajeRequest(pregnancyId, "Tengo sangrado");
        UsuarioAutenticado usuario = new UsuarioAutenticado(patientId, "PACIENTE", clinicaId);

        when(embarazoRepository.findById(pregnancyId)).thenReturn(Optional.of(embarazo));
        when(geminiClient.generarContenido(anyString())).thenReturn(Optional.of("Signo de alarma detectado"));
        when(mensajeChatRepository.save(any(MensajeChat.class))).thenAnswer(invocation -> {
            MensajeChat m = invocation.getArgument(0);
            return m;
        });

        // Simular que normalizar funciona adecuadamente en test
        when(semanticCacheService.normalizar(anyString())).thenReturn("tengo sangrado");

        ChatMensajeResponse response = chatService.enviarMensaje(request, usuario);

        assertNotNull(response);
        assertTrue(response.alarmaProbable());
        assertFalse(response.desdeCache());
        verify(semanticCacheService, never()).buscarEnCache(anyString());
        verify(chatAsyncService).analizarRiesgoAsync(eq(pregnancyId), eq("Tengo sangrado"), eq(true));
        verify(chatAsyncService).crearAlertaSiCorrespondeAsync(eq(pregnancyId), eq("Tengo sangrado"), eq(true));
    }

    @Test
    void enviarMensaje_ReturnsCacheResponse_WithoutCallingGemini() {
        ChatMensajeRequest request = new ChatMensajeRequest(pregnancyId, "consulta normal");
        UsuarioAutenticado usuario = new UsuarioAutenticado(patientId, "PACIENTE", clinicaId);

        when(embarazoRepository.findById(pregnancyId)).thenReturn(Optional.of(embarazo));
        when(semanticCacheService.buscarEnCache(anyString())).thenReturn(Optional.of("Respuesta guardada en caché"));
        when(mensajeChatRepository.save(any(MensajeChat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMensajeResponse response = chatService.enviarMensaje(request, usuario);

        assertNotNull(response);
        assertEquals("Respuesta guardada en caché", response.respuesta());
        assertTrue(response.desdeCache());
        verify(geminiClient, never()).generarContenido(anyString());
    }

    @Test
    void enviarMensaje_UsesFallback_WhenGeminiFails() {
        ChatMensajeRequest request = new ChatMensajeRequest(pregnancyId, "consulta normal");
        UsuarioAutenticado usuario = new UsuarioAutenticado(patientId, "PACIENTE", clinicaId);

        when(embarazoRepository.findById(pregnancyId)).thenReturn(Optional.of(embarazo));
        when(semanticCacheService.buscarEnCache(anyString())).thenReturn(Optional.empty());
        when(geminiClient.generarContenido(anyString())).thenReturn(Optional.empty()); // Falla
        when(mensajeChatRepository.save(any(MensajeChat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMensajeResponse response = chatService.enviarMensaje(request, usuario);

        assertNotNull(response);
        assertTrue(response.fallbackUsado());
        assertTrue(response.respuesta().contains("Ahora no pude conectarme con la IA"));
    }

    @Test
    void obtenerHistorial_AllowedForPatientOwner() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(patientId, "PACIENTE", clinicaId);
        Page<MensajeChat> page = new PageImpl<>(Collections.emptyList());

        when(embarazoRepository.findById(pregnancyId)).thenReturn(Optional.of(embarazo));
        when(mensajeChatRepository.findByEmbarazoIdOrderByCreatedAtDesc(eq(pregnancyId), any(Pageable.class))).thenReturn(page);

        Page<MensajeChatResponse> response = chatService.obtenerHistorial(pregnancyId, 0, 20, usuario);
        assertNotNull(response);
    }

    @Test
    void obtenerHistorial_ForbiddenForOtherPatient() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(UUID.randomUUID(), "PACIENTE", clinicaId);

        when(embarazoRepository.findById(pregnancyId)).thenReturn(Optional.of(embarazo));

        assertThrows(BusinessRuleException.class, () ->
                chatService.obtenerHistorial(pregnancyId, 0, 20, usuario)
        );
    }

    @Test
    void obtenerHistorial_ForbiddenForMedico() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(UUID.randomUUID(), "MEDICO", clinicaId);

        assertThrows(BusinessRuleException.class, () ->
                chatService.obtenerHistorial(pregnancyId, 0, 20, usuario)
        );
    }

    @Test
    void obtenerResumenClinico_AllowedForMedicoOfSameClinic() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(UUID.randomUUID(), "MEDICO", clinicaId);
        ResumenClinico resumen = new ResumenClinico();
        resumen.setEmbarazoId(pregnancyId);
        resumen.setContenidoResumen("Resumen guardado");
        resumen.setGeneradoPorModelo("Gemini API");

        when(embarazoRepository.findById(pregnancyId)).thenReturn(Optional.of(embarazo));
        when(authService.getClinicaIdByPacienteId(patientId)).thenReturn(clinicaId);
        when(resumenClinicoRepository.findById(pregnancyId)).thenReturn(Optional.of(resumen));

        ResumenClinicoResponse response = chatService.obtenerResumenClinico(pregnancyId, usuario);

        assertNotNull(response);
        assertEquals("Resumen guardado", response.contenidoResumen());
    }

    @Test
    void obtenerResumenClinico_ForbiddenForMedicoOfDifferentClinic() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(UUID.randomUUID(), "MEDICO", UUID.randomUUID()); // Clínica distinta

        when(embarazoRepository.findById(pregnancyId)).thenReturn(Optional.of(embarazo));
        when(authService.getClinicaIdByPacienteId(patientId)).thenReturn(clinicaId);

        assertThrows(BusinessRuleException.class, () ->
                chatService.obtenerResumenClinico(pregnancyId, usuario)
        );
    }
}
