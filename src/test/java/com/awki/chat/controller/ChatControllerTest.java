package com.awki.chat.controller;

import com.awki.auth.service.JwtService;
import com.awki.chat.dto.*;
import com.awki.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private UUID embarazoId;
    private UUID pacienteId;
    private String token;

    @BeforeEach
    void setUp() {
        embarazoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        token = "mock-jwt-token";

        Mockito.when(jwtService.extractUserId(eq(token))).thenReturn(pacienteId.toString());
        Mockito.when(jwtService.extractRol(eq(token))).thenReturn("PACIENTE");
        Mockito.when(jwtService.extractClinicaId(eq(token))).thenReturn(UUID.randomUUID().toString());
    }

    @Test
    void enviarMensaje_Success() throws Exception {
        ChatMensajeRequest request = new ChatMensajeRequest(embarazoId, "Hola, tengo una consulta");
        ChatMensajeResponse response = new ChatMensajeResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Esta es la respuesta de la IA",
                false,
                false,
                false,
                LocalDateTime.now()
        );

        Mockito.when(chatService.enviarMensaje(any(ChatMensajeRequest.class), any(UsuarioAutenticado.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/mensaje")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.respuesta").value("Esta es la respuesta de la IA"))
                .andExpect(jsonPath("$.data.desdeCache").value(false));
    }

    @Test
    void obtenerHistorial_Success() throws Exception {
        MensajeChatResponse msg = new MensajeChatResponse(
                UUID.randomUUID(),
                embarazoId,
                "PACIENTE",
                "Mi consulta",
                false,
                false,
                false,
                LocalDateTime.now()
        );
        Page<MensajeChatResponse> page = new PageImpl<>(Collections.singletonList(msg));

        Mockito.when(chatService.obtenerHistorial(eq(embarazoId), eq(0), eq(20), any(UsuarioAutenticado.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/chat/historial")
                        .header("Authorization", "Bearer " + token)
                        .param("embarazoId", embarazoId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].contenido").value("Mi consulta"));
    }

    @Test
    void obtenerResumenClinico_Success() throws Exception {
        ResumenClinicoResponse response = new ResumenClinicoResponse(
                embarazoId,
                "Paciente estable, sin signos de alerta",
                "Gemini API",
                LocalDateTime.now()
        );

        Mockito.when(chatService.obtenerResumenClinico(eq(embarazoId), any(UsuarioAutenticado.class)))
                .thenReturn(response);

        // Cambiar rol de token a MEDICO
        Mockito.when(jwtService.extractRol(eq(token))).thenReturn("MEDICO");

        mockMvc.perform(get("/api/v1/chat/resumen-clinico/" + embarazoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contenidoResumen").value("Paciente estable, sin signos de alerta"));
    }

    @Test
    void regenerarResumen_Success() throws Exception {
        Mockito.when(jwtService.extractRol(eq(token))).thenReturn("MEDICO");

        mockMvc.perform(post("/api/v1/chat/regenerar-resumen/" + embarazoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));

        Mockito.verify(chatService).regenerarResumen(eq(embarazoId), any(UsuarioAutenticado.class));
    }
}
