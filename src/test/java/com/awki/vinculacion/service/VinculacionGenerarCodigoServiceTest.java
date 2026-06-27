package com.awki.vinculacion.service;

import com.awki.auth.entity.Usuario;
import com.awki.auth.repository.UsuarioRepository;
import com.awki.common.enums.RolUsuario;
import com.awki.exception.BusinessRuleException;
import com.awki.vinculacion.dto.CodigoVinculacionData;
import com.awki.vinculacion.dto.UsarCodigoRequest;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VinculacionUsarCodigoServiceTest {

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
    void usarCodigo_conMismoRol_debeLanzarBusinessRuleException() throws Exception {
        UUID usuarioActualId = UUID.randomUUID();
        UUID usuarioGeneradorId = UUID.randomUUID();

        Usuario usuarioActual = new Usuario();
        ReflectionTestUtils.setField(usuarioActual, "id", usuarioActualId);
        usuarioActual.setRol(RolUsuario.PACIENTE);

        CodigoVinculacionData data = new CodigoVinculacionData(
                usuarioGeneradorId,
                RolUsuario.PACIENTE,
                null
        );

        autenticar(usuarioActualId, "PACIENTE");

        when(usuarioRepository.findById(usuarioActualId)).thenReturn(Optional.of(usuarioActual));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("vinculo:codigo:4KR9MT2X")).thenReturn("json");
        when(objectMapper.readValue("json", CodigoVinculacionData.class)).thenReturn(data);

        assertThatThrownBy(() -> vinculacionService.usarCodigo(new UsarCodigoRequest("4KR9MT2X")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede vincular usuarios con el mismo rol")
                .extracting("errorCode")
                .isEqualTo("SAME_ROLE_LINK_FORBIDDEN");

        verify(vinculoRepository, never()).save(any());
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