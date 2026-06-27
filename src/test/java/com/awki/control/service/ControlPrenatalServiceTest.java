package com.awki.control.service;

import com.awki.auth.entity.Medico;
import com.awki.common.enums.EstadoEmbarazo;
import com.awki.common.enums.NivelRiesgo;
import com.awki.control.dto.ControlPrenatalRequest;
import com.awki.control.dto.ControlPrenatalResponse;
import com.awki.control.repository.ControlPrenatalRepository;
import com.awki.control.repository.MedicoControlRepository;
import com.awki.embarazo.entity.Embarazo;
import com.awki.embarazo.service.EmbarazoService;
import com.awki.exception.BusinessRuleException;
import com.awki.riesgo.dto.ResultadoRiesgo;
import com.awki.riesgo.service.MotorRiesgoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControlPrenatalServiceTest {

    @Mock
    private ControlPrenatalRepository controlPrenatalRepository;

    @Mock
    private EmbarazoService embarazoService;

    @Mock
    private MedicoControlRepository medicoControlRepository;

    @Mock
    private MotorRiesgoService motorRiesgoService;

    @InjectMocks
    private ControlPrenatalService controlPrenatalService;

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearControl_conDatosValidos_debeGuardarControlYCalcularRiesgo() {
        UUID embarazoId = UUID.randomUUID();
        UUID medicoUsuarioId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();

        Embarazo embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);
        embarazo.setEstado(EstadoEmbarazo.ACTIVO);

        Medico medico = new Medico();
        ReflectionTestUtils.setField(medico, "id", medicoId);

        ControlPrenatalRequest request = requestValido(embarazoId);

        ResultadoRiesgo resultadoRiesgo = new ResultadoRiesgo(
                NivelRiesgo.VERDE,
                List.of(),
                false,
                "Sin criterios de riesgo detectados."
        );

        autenticar(medicoUsuarioId, "MEDICO");

        when(embarazoService.getEmbarazoEntityById(embarazoId)).thenReturn(embarazo);
        when(medicoControlRepository.findByUsuarioId(medicoUsuarioId)).thenReturn(Optional.of(medico));
        when(controlPrenatalRepository.countByEmbarazo_Id(embarazoId)).thenReturn(0L);
        when(motorRiesgoService.evaluarDesdeControl(eq(embarazoId), any()))
                .thenReturn(resultadoRiesgo);
        when(controlPrenatalRepository.save(any())).thenAnswer(invocation -> {
            com.awki.control.entity.ControlPrenatal control = invocation.getArgument(0);
            ReflectionTestUtils.setField(control, "id", UUID.randomUUID());
            return control;
        });

        ControlPrenatalResponse response = controlPrenatalService.crearControl(request);

        assertThat(response.controlId()).isNotNull();
        assertThat(response.nivelRiesgoCalculado()).isEqualTo(NivelRiesgo.VERDE);
        assertThat(response.imcCalculado()).isEqualByComparingTo("24.21");
        assertThat(response.semanasGestacion()).isEqualTo(28);

        verify(motorRiesgoService).evaluarDesdeControl(eq(embarazoId), any());
        verify(controlPrenatalRepository).save(any());
    }

    @Test
    void crearControl_conEmbarazoNoActivo_debeLanzarBusinessRuleException() {
        UUID embarazoId = UUID.randomUUID();

        Embarazo embarazo = new Embarazo();
        ReflectionTestUtils.setField(embarazo, "id", embarazoId);
        embarazo.setEstado(EstadoEmbarazo.FINALIZADO_PARTO);

        when(embarazoService.getEmbarazoEntityById(embarazoId)).thenReturn(embarazo);

        assertThatThrownBy(() -> controlPrenatalService.crearControl(requestValido(embarazoId)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Solo se pueden registrar controles en embarazos activos")
                .extracting("errorCode")
                .isEqualTo("PREGNANCY_NOT_ACTIVE");

        verify(motorRiesgoService, never()).evaluarDesdeControl(any(), any());
        verify(controlPrenatalRepository, never()).save(any());
    }

    private ControlPrenatalRequest requestValido(UUID embarazoId) {
        return new ControlPrenatalRequest(
                embarazoId,
                LocalDate.now(),
                28,
                BigDecimal.valueOf(68.5),
                BigDecimal.valueOf(168.2),
                110,
                70,
                BigDecimal.valueOf(28.0),
                140,
                null,
                BigDecimal.valueOf(11.2),
                null,
                null,
                null,
                null,
                LocalDate.now().plusWeeks(4),
                "Control normal",
                null,
                false
        );
    }

    private void autenticar(UUID usuarioId, String rol) {
        var authentication = new UsernamePasswordAuthenticationToken(
                usuarioId.toString(),
                null,
                List.of(() -> "ROLE_" + rol)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}