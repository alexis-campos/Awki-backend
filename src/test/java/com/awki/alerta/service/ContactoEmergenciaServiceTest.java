package com.awki.alerta.service;

import com.awki.alerta.dto.ContactoEmergenciaRequest;
import com.awki.alerta.dto.ContactoEmergenciaResponse;
import com.awki.alerta.entity.CanalPreferido;
import com.awki.alerta.entity.ContactoEmergencia;
import com.awki.alerta.repository.ContactoEmergenciaRepository;
import com.awki.auth.entity.Paciente;
import com.awki.auth.repository.PacienteRepository;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.exception.TenantViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactoEmergenciaServiceTest {

    @Mock
    private ContactoEmergenciaRepository contactoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private ContactoEmergenciaService contactoService;

    private UUID userId;
    private UUID pacienteId;
    private Paciente paciente;
    private UsuarioAutenticado authUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();

        paciente = new Paciente();
        ReflectionTestUtils.setField(paciente, "id", pacienteId);

        authUser = new UsuarioAutenticado(userId, "PACIENTE", null);
    }

    @Test
    void testAgregarContactoExito() {
        ContactoEmergenciaRequest req = new ContactoEmergenciaRequest("Mamá", "+51999888777", "Madre", CanalPreferido.SMS);

        when(pacienteRepository.findByUsuario_Id(userId)).thenReturn(Optional.of(paciente));
        
        ContactoEmergencia mockContacto = new ContactoEmergencia();
        ReflectionTestUtils.setField(mockContacto, "id", UUID.randomUUID());
        mockContacto.setNombre(req.nombre());
        mockContacto.setTelefono(req.telefono());
        mockContacto.setParentesco(req.parentesco());
        mockContacto.setCanalPreferido(req.canalPreferido());
        mockContacto.setActivo(true);
        
        when(contactoRepository.save(any(ContactoEmergencia.class))).thenReturn(mockContacto);

        ContactoEmergenciaResponse response = contactoService.agregarContacto(req, authUser);

        assertNotNull(response);
        assertEquals("Mamá", response.nombre());
        assertEquals("+51999888777", response.telefono());
        verify(contactoRepository, times(1)).save(any(ContactoEmergencia.class));
    }

    @Test
    void testEliminarContactoExito() {
        UUID contactoId = UUID.randomUUID();
        ContactoEmergencia mockContacto = new ContactoEmergencia();
        ReflectionTestUtils.setField(mockContacto, "id", contactoId);
        mockContacto.setPaciente(paciente);
        mockContacto.setActivo(true);

        when(pacienteRepository.findByUsuario_Id(userId)).thenReturn(Optional.of(paciente));
        when(contactoRepository.findById(contactoId)).thenReturn(Optional.of(mockContacto));

        contactoService.eliminarContacto(contactoId, authUser);

        assertFalse(mockContacto.isActivo());
        verify(contactoRepository, times(1)).save(mockContacto);
    }

    @Test
    void testEliminarContactoAjenoLanzaException() {
        UUID contactoId = UUID.randomUUID();
        
        Paciente otroPaciente = new Paciente();
        ReflectionTestUtils.setField(otroPaciente, "id", UUID.randomUUID());
        
        ContactoEmergencia mockContacto = new ContactoEmergencia();
        ReflectionTestUtils.setField(mockContacto, "id", contactoId);
        mockContacto.setPaciente(otroPaciente); // Dueño diferente
        mockContacto.setActivo(true);

        when(pacienteRepository.findByUsuario_Id(userId)).thenReturn(Optional.of(paciente));
        when(contactoRepository.findById(contactoId)).thenReturn(Optional.of(mockContacto));

        assertThrows(TenantViolationException.class, () -> contactoService.eliminarContacto(contactoId, authUser));
        verify(contactoRepository, never()).save(any(ContactoEmergencia.class));
    }
}
