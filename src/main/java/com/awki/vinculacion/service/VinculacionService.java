package com.awki.vinculacion.service;

import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.auth.entity.Usuario;
import com.awki.auth.repository.UsuarioRepository;
import com.awki.common.enums.EstadoVinculo;
import com.awki.common.enums.ModoUso;
import com.awki.common.enums.RolUsuario;
import com.awki.exception.BusinessRuleException;
import com.awki.exception.ResourceNotFoundException;
import com.awki.vinculacion.dto.CodigoVinculacionData;
import com.awki.vinculacion.dto.GenerarCodigoResponse;
import com.awki.vinculacion.dto.UsarCodigoRequest;
import com.awki.vinculacion.dto.VinculoResponse;
import com.awki.vinculacion.entity.VinculoMedicoPaciente;
import com.awki.vinculacion.repository.MedicoVinculacionRepository;
import com.awki.vinculacion.repository.PacienteVinculacionRepository;
import com.awki.vinculacion.repository.VinculoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VinculacionService {

    private static final long CODIGO_TTL_SEGUNDOS = 172800;
    private static final String CODIGO_PREFIX = "vinculo:codigo:";
    private static final String CODIGO_USUARIO_PREFIX = "vinculo:usuario:";
    private static final String CARACTERES_PERMITIDOS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final VinculoRepository vinculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoVinculacionRepository medicoRepository;
    private final PacienteVinculacionRepository pacienteRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    public GenerarCodigoResponse generarCodigo() {
        Usuario usuario = obtenerUsuarioAutenticado();

        validarRolGenerador(usuario.getRol());

        String usuarioKey = CODIGO_USUARIO_PREFIX + usuario.getId();
        String codigoExistente = stringRedisTemplate.opsForValue().get(usuarioKey);

        if (codigoExistente != null) {
            return new GenerarCodigoResponse(
                    codigoExistente,
                    LocalDateTime.now().plusSeconds(obtenerTtl(usuarioKey)),
                    usuario.getRol()
            );
        }

        String codigo = generarCodigoSeguro();

        CodigoVinculacionData data = new CodigoVinculacionData(
                usuario.getId(),
                usuario.getRol(),
                usuario.getClinicaId()
        );

        guardarCodigoEnRedis(codigo, usuarioKey, data);

        return new GenerarCodigoResponse(
                codigo,
                LocalDateTime.now().plusSeconds(CODIGO_TTL_SEGUNDOS),
                usuario.getRol()
        );
    }

    @Transactional
    public VinculoResponse usarCodigo(UsarCodigoRequest request) {
        Usuario usuarioActual = obtenerUsuarioAutenticado();

        String codigo = request.codigo().toUpperCase();
        String codigoKey = CODIGO_PREFIX + codigo;
        String json = stringRedisTemplate.opsForValue().get(codigoKey);

        if (json == null) {
            throw new BusinessRuleException(
                    "LINK_CODE_INVALID_OR_EXPIRED",
                    "El código de vinculación no existe o expiró"
            );
        }

        CodigoVinculacionData data = leerCodigoData(json);

        if (usuarioActual.getRol() == data.rol()) {
            throw new BusinessRuleException(
                    "SAME_ROLE_LINK_FORBIDDEN",
                    "No se puede vincular usuarios con el mismo rol"
            );
        }

        VinculoMedicoPaciente vinculo = crearVinculo(usuarioActual, data);

        stringRedisTemplate.delete(codigoKey);
        stringRedisTemplate.delete(CODIGO_USUARIO_PREFIX + data.generadoPor());

        return toResponse(vinculo);
    }

    @Transactional(readOnly = true)
    public List<VinculoResponse> misVinculos() {
        Medico medico = obtenerMedicoDelUsuarioAutenticado();

        return vinculoRepository.findByMedico_IdAndEstado(medico.getId(), EstadoVinculo.ACTIVO)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VinculoResponse finalizarVinculo(UUID vinculoId) {
        VinculoMedicoPaciente vinculo = vinculoRepository.findById(vinculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo", vinculoId.toString()));

        if (vinculo.getEstado() == EstadoVinculo.FINALIZADO) {
            return toResponse(vinculo);
        }

        vinculo.setEstado(EstadoVinculo.FINALIZADO);
        vinculo.setFinalizadoAt(LocalDateTime.now());

        Paciente paciente = vinculo.getPaciente();
        paciente.setModoUso(ModoUso.AUTONOMO);
        pacienteRepository.save(paciente);

        return toResponse(vinculoRepository.save(vinculo));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> obtenerMedicoVinculadoActivo(UUID pacienteId) {
        return vinculoRepository.findByPaciente_IdAndEstado(pacienteId, EstadoVinculo.ACTIVO)
                .map(v -> v.getMedico().getId());
    }

    private VinculoMedicoPaciente crearVinculo(Usuario usuarioActual, CodigoVinculacionData data) {
        Medico medico;
        Paciente paciente;

        if (usuarioActual.getRol() == RolUsuario.MEDICO) {
            medico = medicoRepository.findByUsuarioId(usuarioActual.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Médico", usuarioActual.getId().toString()));

            paciente = pacienteRepository.findByUsuarioId(data.generadoPor())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente", data.generadoPor().toString()));
        } else {
            paciente = pacienteRepository.findByUsuarioId(usuarioActual.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente", usuarioActual.getId().toString()));

            medico = medicoRepository.findByUsuarioId(data.generadoPor())
                    .orElseThrow(() -> new ResourceNotFoundException("Médico", data.generadoPor().toString()));
        }

        if (vinculoRepository.existsByPaciente_IdAndEstado(paciente.getId(), EstadoVinculo.ACTIVO)) {
            throw new BusinessRuleException(
                    "PATIENT_ALREADY_LINKED",
                    "La paciente ya tiene un vínculo activo con otro médico"
            );
        }

        VinculoMedicoPaciente vinculo = new VinculoMedicoPaciente();
        vinculo.setMedico(medico);
        vinculo.setPaciente(paciente);
        vinculo.setClinicaId(medico.getClinicaId());
        vinculo.setEstado(EstadoVinculo.ACTIVO);
        vinculo.setVinculadoAt(LocalDateTime.now());

        paciente.setModoUso(ModoUso.VINCULADA);
        if (paciente.getUsuario() != null) {
            paciente.getUsuario().setClinicaId(medico.getClinicaId());
            usuarioRepository.save(paciente.getUsuario());
        }
        pacienteRepository.save(paciente);

        return vinculoRepository.save(vinculo);
    }

    private Usuario obtenerUsuarioAutenticado() {
        String usuarioId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();

        return usuarioRepository.findById(UUID.fromString(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
    }

    private Medico obtenerMedicoDelUsuarioAutenticado() {
        Usuario usuario = obtenerUsuarioAutenticado();

        return medicoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico", usuario.getId().toString()));
    }

    private void validarRolGenerador(RolUsuario rol) {
        if (rol != RolUsuario.PACIENTE && rol != RolUsuario.MEDICO) {
            throw new BusinessRuleException(
                    "ROLE_NOT_ALLOWED",
                    "Solo pacientes y médicos pueden generar códigos de vinculación"
            );
        }
    }

    private String generarCodigoSeguro() {
        String codigo;

        do {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < 8; i++) {
                int index = secureRandom.nextInt(CARACTERES_PERMITIDOS.length());
                builder.append(CARACTERES_PERMITIDOS.charAt(index));
            }

            codigo = builder.toString();
        } while (Boolean.TRUE.equals(stringRedisTemplate.hasKey(CODIGO_PREFIX + codigo)));

        return codigo;
    }

    private void guardarCodigoEnRedis(String codigo, String usuarioKey, CodigoVinculacionData data) {
        try {
            String json = objectMapper.writeValueAsString(data);

            stringRedisTemplate.opsForValue().set(
                    CODIGO_PREFIX + codigo,
                    json,
                    Duration.ofSeconds(CODIGO_TTL_SEGUNDOS)
            );

            stringRedisTemplate.opsForValue().set(
                    usuarioKey,
                    codigo,
                    Duration.ofSeconds(CODIGO_TTL_SEGUNDOS)
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException(
                    "LINK_CODE_SERIALIZATION_ERROR",
                    "No se pudo generar el código de vinculación"
            );
        }
    }

    private CodigoVinculacionData leerCodigoData(String json) {
        try {
            return objectMapper.readValue(json, CodigoVinculacionData.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException(
                    "LINK_CODE_INVALID_DATA",
                    "El código de vinculación contiene datos inválidos"
            );
        }
    }

    private long obtenerTtl(String key) {
        Long ttl = stringRedisTemplate.getExpire(key);
        return ttl == null || ttl < 0 ? CODIGO_TTL_SEGUNDOS : ttl;
    }

    private VinculoResponse toResponse(VinculoMedicoPaciente vinculo) {
        int edad = 0;
        if (vinculo.getPaciente().getFechaNacimiento() != null) {
            edad = (int) java.time.temporal.ChronoUnit.YEARS.between(
                    vinculo.getPaciente().getFechaNacimiento(),
                    java.time.LocalDate.now()
            );
        }

        return new VinculoResponse(
                vinculo.getId(),
                vinculo.getMedico().getId(),
                vinculo.getPaciente().getId(),
                vinculo.getClinicaId(),
                vinculo.getEstado(),
                vinculo.getVinculadoAt(),
                vinculo.getFinalizadoAt(),
                vinculo.getPaciente().getNombres(),
                vinculo.getPaciente().getApellidos(),
                vinculo.getPaciente().getDni(),
                edad,
                vinculo.getMedico().getNombres(),
                vinculo.getMedico().getApellidos()
        );
    }
}
