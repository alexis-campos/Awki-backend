package com.awki.vinculacion.service;

import com.awki.auth.entity.Usuario;
import com.awki.auth.repository.UsuarioRepository;
import com.awki.common.enums.RolUsuario;
import com.awki.vinculacion.dto.GenerarCodigoResponse;
import com.awki.vinculacion.repository.MedicoVinculacionRepository;
import com.awki.vinculacion.repository.PacienteVinculacionRepository;
import com.awki.vinculacion.repository.VinculoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VinculacionServiceTest {

    @Mock
    private VinculoRepository vinculoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MedicoVinculacionRepository medicoRepository;

    @Mock
    private PacienteVinculacionRepository pacienteRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VinculacionService vinculacionService;

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generarCodigo_debeCrearCodigoDe8Caracteres() throws Exception {
        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        usuario.setRol(RolUsuario.PACIENTE);

        autenticar(usuarioId, "PACIENTE");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("vinculo:usuario:" + usuarioId)).thenReturn(null);
        when(stringRedisTemplate.hasKey(startsWith("vinculo:codigo:"))).thenReturn(false);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"generadoPor\":\"" + usuarioId + "\",\"rol\":\"PACIENTE\",\"clinicaId\":null}");

        GenerarCodigoResponse response = vinculacionService.generarCodigo();

        assertThat(response.codigo()).hasSize(8);
        assertThat(response.generadoPor()).isEqualTo(RolUsuario.PACIENTE);
        assertThat(response.expiraAt()).isNotNull();

        verify(valueOperations).set(
                startsWith("vinculo:codigo:"),
                contains("\"rol\":\"PACIENTE\""),
                any()
        );

        verify(valueOperations).set(
                eq("vinculo:usuario:" + usuarioId),
                eq(response.codigo()),
                any()
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