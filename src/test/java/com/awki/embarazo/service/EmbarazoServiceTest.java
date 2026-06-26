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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbarazoServiceTest {

    @Mock
    private EmbarazoRepository embarazoRepository;

    @Mock
    private AntecedentesRepository antecedentesRepository;

    @Mock
    private AuthService authService;

    @Mock
    private VinculacionService vinculacionService;

    @InjectMocks
    private EmbarazoService embarazoService;

    private UUID pacienteId;
    private UUID embarazoId;
    private UUID medicoId;

    @BeforeEach
    void setUp() {
        pacienteId = UUID.randomUUID();
        embarazoId = UUID.randomUUID();
        medicoId = UUID.randomUUID();
    }

    @Test
    void crearEmbarazo_Success_NormalAge_LinkedDoctor() {
        LocalDate fum = LocalDate.now().minusWeeks(10);
        LocalDate fechaNacimiento = LocalDate.now().minusYears(25); // 25 years old

        EmbarazoRequest request = new EmbarazoRequest(
                pacienteId,
                fum,
                null,
                1,
                0,
                0,
                0,
                false
        );

        when(embarazoRepository.findByPacienteIdAndEstado(pacienteId, EstadoEmbarazo.ACTIVO))
                .thenReturn(Optional.empty());
        when(vinculacionService.obtenerMedicoVinculadoActivo(pacienteId))
                .thenReturn(Optional.of(medicoId));
        when(authService.getFechaNacimientoPaciente(pacienteId))
                .thenReturn(fechaNacimiento);

        when(embarazoRepository.save(any(Embarazo.class))).thenAnswer(invocation -> {
            Embarazo e = invocation.getArgument(0);
            ReflectionTestUtils.setField(e, "id", embarazoId);
            return e;
        });

        when(antecedentesRepository.save(any(AntecedentesClinicos.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmbarazoResponse response = embarazoService.crearEmbarazo(request);

        assertNotNull(response);
        assertEquals(embarazoId, response.id());
        assertEquals(pacienteId, response.pacienteId());
        assertEquals(medicoId, response.medicoId());
        assertEquals(fum, response.fechaUltimaMenstruacion());
        assertEquals(fum.plusDays(280), response.fechaProbableParto());
        assertEquals(10L, response.semanasGestacionActuales());
        assertEquals(1, response.trimestre());
        assertFalse(response.embarazoMultiple());

        verify(embarazoRepository).save(any(Embarazo.class));
        verify(antecedentesRepository).save(any(AntecedentesClinicos.class));
    }

    @Test
    void crearEmbarazo_ThrowsException_WhenActivePregnancyExists() {
        EmbarazoRequest request = new EmbarazoRequest(
                pacienteId,
                LocalDate.now().minusWeeks(10),
                null,
                1,
                0,
                0,
                0,
                false
        );

        when(embarazoRepository.findByPacienteIdAndEstado(pacienteId, EstadoEmbarazo.ACTIVO))
                .thenReturn(Optional.of(new Embarazo()));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                embarazoService.crearEmbarazo(request)
        );

        assertEquals("ACTIVE_PREGNANCY_EXISTS", exception.getErrorCode());
        verify(embarazoRepository, never()).save(any(Embarazo.class));
    }

    @Test
    void crearEmbarazo_ThrowsException_WhenInvalidFUM() {
        EmbarazoRequest request = new EmbarazoRequest(
                pacienteId,
                LocalDate.now().minusWeeks(50), // 50 weeks ago (invalid)
                null,
                1,
                0,
                0,
                0,
                false
        );

        when(embarazoRepository.findByPacienteIdAndEstado(pacienteId, EstadoEmbarazo.ACTIVO))
                .thenReturn(Optional.empty());

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                embarazoService.crearEmbarazo(request)
        );

        assertEquals("INVALID_FUM", exception.getErrorCode());
        verify(embarazoRepository, never()).save(any(Embarazo.class));
    }

    @Test
    void crearEmbarazo_SetsAgeRisk_WhenMotherIsOlderThan35() {
        LocalDate fum = LocalDate.now().minusWeeks(8);
        LocalDate fechaNacimiento = LocalDate.now().minusYears(36); // 36 years old

        EmbarazoRequest request = new EmbarazoRequest(
                pacienteId,
                fum,
                null,
                1,
                0,
                0,
                0,
                false
        );

        when(embarazoRepository.findByPacienteIdAndEstado(pacienteId, EstadoEmbarazo.ACTIVO))
                .thenReturn(Optional.empty());
        when(vinculacionService.obtenerMedicoVinculadoActivo(pacienteId))
                .thenReturn(Optional.empty());
        when(authService.getFechaNacimientoPaciente(pacienteId))
                .thenReturn(fechaNacimiento);

        when(embarazoRepository.save(any(Embarazo.class))).thenAnswer(invocation -> {
            Embarazo e = invocation.getArgument(0);
            ReflectionTestUtils.setField(e, "id", embarazoId);
            return e;
        });

        when(antecedentesRepository.save(any(AntecedentesClinicos.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmbarazoResponse response = embarazoService.crearEmbarazo(request);

        assertNotNull(response);
        verify(antecedentesRepository).save(argThat(antecedentes -> {
            assertTrue(antecedentes.isEdadMaternaRiesgo());
            return true;
        }));
    }

    @Test
    void crearOActualizarAntecedentes_Success() {
        Embarazo embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);
        embarazo.setPacienteId(pacienteId);

        AntecedentesRequest request = new AntecedentesRequest(
                true, // diabetesPrevia
                false, // hipertensionPrevia
                true, // preeclampsiaPrevia
                false, // enfermedadRenal
                false, // enfermedadAutoinmune
                true, // anemiaPrevia
                false, // vihPositivo
                false, // sifilisPrevia
                false, // trastornoCoagulacion
                true, // residenciaAltitud
                true, // obesidadPregestacional
                "Alguna observacion"
        );

        when(embarazoRepository.findById(embarazoId)).thenReturn(Optional.of(embarazo));
        when(antecedentesRepository.save(any(AntecedentesClinicos.class))).thenAnswer(invocation -> {
            AntecedentesClinicos ac = invocation.getArgument(0);
            if (ac.getId() == null && ac.getEmbarazo() != null) {
                ac.setId(ac.getEmbarazo().getId());
            }
            return ac;
        });

        AntecedentesResponse response = embarazoService.crearOActualizarAntecedentes(embarazoId, request);

        assertNotNull(response);
        assertEquals(embarazoId, response.embarazoId());
        assertTrue(response.diabetesPrevia());
        assertFalse(response.hipertensionPrevia());
        assertTrue(response.preeclampsiaPrevia());
        assertTrue(response.anemiaPrevia());
        assertTrue(response.residenciaAltitud());
        assertTrue(response.obesidadPregestacional());
        assertEquals("Alguna observacion", response.observaciones());
    }

    @Test
    void finalizarEmbarazo_Success() {
        Embarazo embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);
        embarazo.setFechaUltimaMenstruacion(LocalDate.now().minusWeeks(39));
        embarazo.setEstado(EstadoEmbarazo.ACTIVO);

        FinalizarEmbarazoRequest request = new FinalizarEmbarazoRequest(
                EstadoEmbarazo.FINALIZADO_PARTO,
                LocalDate.now()
        );

        when(embarazoRepository.findById(embarazoId)).thenReturn(Optional.of(embarazo));
        when(embarazoRepository.save(any(Embarazo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmbarazoResponse response = embarazoService.finalizarEmbarazo(embarazoId, request);

        assertNotNull(response);
        assertEquals(EstadoEmbarazo.FINALIZADO_PARTO, response.estado());
        assertEquals(LocalDate.now(), response.fechaFin());
        verify(embarazoRepository).save(embarazo);
    }

    @Test
    void finalizarEmbarazo_ThrowsException_WhenTargetStateIsActivo() {
        Embarazo embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);

        FinalizarEmbarazoRequest request = new FinalizarEmbarazoRequest(
                EstadoEmbarazo.ACTIVO,
                LocalDate.now()
        );

        when(embarazoRepository.findById(embarazoId)).thenReturn(Optional.of(embarazo));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                embarazoService.finalizarEmbarazo(embarazoId, request)
        );

        assertEquals("INVALID_STATE", exception.getErrorCode());
        verify(embarazoRepository, never()).save(any(Embarazo.class));
    }
}
