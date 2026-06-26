package com.awki.embarazo.controller;

import com.awki.auth.service.JwtService;
import com.awki.common.enums.EstadoEmbarazo;
import com.awki.embarazo.dto.*;
import com.awki.embarazo.service.EmbarazoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmbarazoController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmbarazoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmbarazoService embarazoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private UUID pacienteId;
    private UUID embarazoId;
    private String token;

    @BeforeEach
    void setUp() {
        pacienteId = UUID.randomUUID();
        embarazoId = UUID.randomUUID();
        token = "mock-jwt-token";
        Mockito.when(jwtService.extractClinicaId(eq(token))).thenReturn(UUID.randomUUID().toString());
        Mockito.when(jwtService.extractUserId(eq(token))).thenReturn(pacienteId.toString());
        Mockito.when(jwtService.extractRol(eq(token))).thenReturn("PACIENTE");
    }

    @Test
    void crearEmbarazo_Success() throws Exception {
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

        EmbarazoResponse response = new EmbarazoResponse(
                embarazoId,
                pacienteId,
                null,
                request.fechaUltimaMenstruacion(),
                request.fechaUltimaMenstruacion().plusDays(280),
                null,
                10,
                10L,
                1,
                1,
                0,
                0,
                0,
                false,
                EstadoEmbarazo.ACTIVO,
                null
        );

        Mockito.when(embarazoService.crearEmbarazo(any(EmbarazoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/embarazos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(embarazoId.toString()))
                .andExpect(jsonPath("$.data.semanasGestacionActuales").value(10));
    }

    @Test
    void getEmbarazoActivo_Success() throws Exception {
        EmbarazoResponse response = new EmbarazoResponse(
                embarazoId,
                pacienteId,
                null,
                LocalDate.now().minusWeeks(12),
                LocalDate.now().plusWeeks(28),
                null,
                12,
                12L,
                1,
                1,
                0,
                0,
                0,
                false,
                EstadoEmbarazo.ACTIVO,
                null
        );

        Mockito.when(embarazoService.obtenerEmbarazoActivo(eq(pacienteId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/embarazos/activo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.data.semanasGestacionActuales").value(12));
    }

    @Test
    void getEmbarazoPorId_Success() throws Exception {
        EmbarazoResponse response = new EmbarazoResponse(
                embarazoId,
                pacienteId,
                null,
                LocalDate.now().minusWeeks(15),
                LocalDate.now().plusWeeks(25),
                null,
                15,
                15L,
                2,
                1,
                0,
                0,
                0,
                false,
                EstadoEmbarazo.ACTIVO,
                null
        );

        Mockito.when(embarazoService.obtenerEmbarazoPorId(eq(embarazoId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/embarazos/" + embarazoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(embarazoId.toString()))
                .andExpect(jsonPath("$.data.trimestre").value(2));
    }

    @Test
    void actualizarAntecedentes_Success() throws Exception {
        AntecedentesRequest request = new AntecedentesRequest(
                true, false, false, false, false, true, false, false, false, false, false, "Observaciones de prueba"
        );

        AntecedentesResponse response = new AntecedentesResponse(
                embarazoId,
                true,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                "Observaciones de prueba"
        );

        Mockito.when(embarazoService.crearOActualizarAntecedentes(eq(embarazoId), any(AntecedentesRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/embarazos/" + embarazoId + "/antecedentes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.diabetesPrevia").value(true))
                .andExpect(jsonPath("$.data.anemiaPrevia").value(true))
                .andExpect(jsonPath("$.data.observaciones").value("Observaciones de prueba"));
    }

    @Test
    void finalizarEmbarazo_Success() throws Exception {
        FinalizarEmbarazoRequest request = new FinalizarEmbarazoRequest(
                EstadoEmbarazo.FINALIZADO_PARTO,
                LocalDate.now()
        );

        EmbarazoResponse response = new EmbarazoResponse(
                embarazoId,
                pacienteId,
                null,
                LocalDate.now().minusWeeks(39),
                LocalDate.now().minusWeeks(1),
                null,
                39,
                39L,
                3,
                1,
                0,
                0,
                0,
                false,
                EstadoEmbarazo.FINALIZADO_PARTO,
                LocalDate.now()
        );

        Mockito.when(embarazoService.finalizarEmbarazo(eq(embarazoId), any(FinalizarEmbarazoRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/embarazos/" + embarazoId + "/finalizar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("FINALIZADO_PARTO"))
                .andExpect(jsonPath("$.data.fechaFin").value(LocalDate.now().toString()));
    }
}
