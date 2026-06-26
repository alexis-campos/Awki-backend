package com.awki.clinica.controller;

import com.awki.auth.service.JwtService;
import com.awki.clinica.dto.*;
import com.awki.clinica.service.ClinicaService;
import com.awki.common.enums.EstadoSuscripcion;
import com.awki.common.enums.PlanSaas;
import com.awki.common.enums.TipoClinica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClinicaController.class)
@AutoConfigureMockMvc(addFilters = false) // Deshabilitamos filtros de seguridad para probar sólo la lógica del controlador
class ClinicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClinicaService clinicaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private UUID clinicaId;
    private String token;

    @BeforeEach
    void setUp() {
        clinicaId = UUID.randomUUID();
        token = "mock-jwt-token";
        Mockito.when(jwtService.extractClinicaId(eq(token))).thenReturn(clinicaId.toString());
    }

    @Test
    void getMiClinica_Success() throws Exception {
        ClinicaResponse response = new ClinicaResponse(
                clinicaId,
                "Clínica San Juan",
                TipoClinica.CLINICA_PRIVADA,
                "12345678901",
                "DIRESA LIMA",
                "Lima",
                "Lima",
                "San Isidro",
                BigDecimal.valueOf(-12.094),
                BigDecimal.valueOf(-77.016),
                PlanSaas.BASICO,
                EstadoSuscripcion.ACTIVO,
                100,
                5
        );

        Mockito.when(clinicaService.getClinicaById(eq(clinicaId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/clinica/mi-clinica")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Clínica San Juan"))
                .andExpect(jsonPath("$.data.planSaas").value("BASICO"));
    }

    @Test
    void getMedicos_Success() throws Exception {
        List<ClinicaMedicoResponse> medicos = List.of(
                new ClinicaMedicoResponse(UUID.randomUUID(), "Carlos", "Ramirez", "CMP12345", "OBSTETRA", true, 5L)
        );

        Mockito.when(clinicaService.getMedicosByClinica(eq(clinicaId))).thenReturn(medicos);

        mockMvc.perform(get("/api/v1/clinica/medicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nombres").value("Carlos"))
                .andExpect(jsonPath("$.data[0].pacientesVinculadas").value(5));
    }

    @Test
    void cambiarEstadoMedico_Success() throws Exception {
        UUID medicoId = UUID.randomUUID();

        Mockito.doNothing().when(clinicaService).cambiarEstadoMedico(eq(clinicaId), eq(medicoId), eq(false));

        mockMvc.perform(patch("/api/v1/clinica/medicos/" + medicoId + "/estado")
                        .header("Authorization", "Bearer " + token)
                        .param("activo", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMetricas_Success() throws Exception {
        ClinicaMetricasResponse metricas = new ClinicaMetricasResponse(12L, 45L, 3L, 12.0);

        Mockito.when(clinicaService.getMetricas(eq(clinicaId))).thenReturn(metricas);

        mockMvc.perform(get("/api/v1/clinica/metricas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pacientesActivas").value(12))
                .andExpect(jsonPath("$.data.controlesMes").value(45))
                .andExpect(jsonPath("$.data.usoPlanPorcentaje").value(12.0));
    }

    @Test
    void getPacientesResumen_Success() throws Exception {
        List<PacienteResumenResponse> pacientes = List.of(
                new PacienteResumenResponse(UUID.randomUUID(), "María", "López", "12345678", "Dr. Carlos Ramirez", "ROJO")
        );

        Mockito.when(clinicaService.getPacientesResumen(eq(clinicaId))).thenReturn(pacientes);

        mockMvc.perform(get("/api/v1/clinica/pacientes-resumen")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nombres").value("María"))
                .andExpect(jsonPath("$.data[0].riesgoActual").value("ROJO"));
    }
}
