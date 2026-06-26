package com.awki.auth.service;

import com.awki.auth.dto.AuthResponse;
import com.awki.auth.dto.LoginRequest;
import com.awki.auth.dto.RegisterMedicoRequest;
import com.awki.auth.dto.RegisterPacienteRequest;
import com.awki.auth.entity.Medico;
import com.awki.auth.entity.Paciente;
import com.awki.auth.entity.Usuario;
import com.awki.auth.repository.MedicoRepository;
import com.awki.auth.repository.PacienteRepository;
import com.awki.auth.repository.UsuarioRepository;
import com.awki.common.enums.ModoUso;
import com.awki.common.enums.RolUsuario;
import com.awki.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.awki.auth.dto.MedicoInfoDto;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Transactional
    public AuthResponse registerPaciente(RegisterPacienteRequest request) {
        if (!Boolean.TRUE.equals(request.consentimientoIa())) {
            throw new ConsentRequiredException("IA", "El consentimiento de uso de IA es obligatorio para registrarse");
        }

        LocalDate hoy = LocalDate.now();
        if (request.fechaNacimiento().isAfter(hoy)) {
            throw new BusinessRuleException("INVALID_BIRTH_DATE", "La fecha de nacimiento no puede ser futura");
        }
        if (request.fechaNacimiento().isBefore(hoy.minusYears(70))) {
            throw new BusinessRuleException("INVALID_BIRTH_DATE", "La fecha de nacimiento no puede ser hace más de 70 años");
        }

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(RolUsuario.PACIENTE);
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);
        paciente.setNombres(request.nombres());
        paciente.setApellidos(request.apellidos());
        paciente.setTelefono(request.telefono());
        paciente.setDni(request.dni());
        paciente.setFechaNacimiento(request.fechaNacimiento());
        paciente.setDepartamento(request.departamento());
        paciente.setModoUso(ModoUso.AUTONOMO);
        paciente.setConsentimientoIa(true);
        paciente.setConsentimientoFecha(LocalDateTime.now());
        pacienteRepository.save(paciente);

        String token = jwtService.generateToken(usuario.getId(), usuario.getRol().name(), null);
        return new AuthResponse(token);
    }

    @Transactional
    public AuthResponse registerMedico(RegisterMedicoRequest request, String currentToken) {
        String clinicaIdStr = jwtService.extractClinicaId(currentToken);
        UUID clinicaId = UUID.fromString(clinicaIdStr);

        if (medicoRepository.existsByCmp(request.cmp())) {
            throw new CmpAlreadyExistsException(request.cmp());
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(RolUsuario.MEDICO);
        usuario.setActivo(true);
        usuario.setClinicaId(clinicaId);
        usuario = usuarioRepository.save(usuario);

        Medico medico = new Medico();
        medico.setUsuario(usuario);
        medico.setClinicaId(clinicaId);
        medico.setNombres(request.nombres());
        medico.setApellidos(request.apellidos());
        medico.setCmp(request.cmp());
        medico.setEspecialidad(request.especialidad());
        medicoRepository.save(medico);

        String token = jwtService.generateToken(usuario.getId(), usuario.getRol().name(), clinicaId);
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

        if (!usuario.isActivo()) {
            throw new AccountDisabledException("La cuenta está desactivada");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(usuario.getId(), usuario.getRol().name(), usuario.getClinicaId());
        return new AuthResponse(token);
    }

    public AuthResponse refresh(String currentToken) {
        Boolean isBlacklisted = redisTemplate.hasKey(BLACKLIST_PREFIX + currentToken);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new TokenBlacklistedException("El token ha sido invalidado");
        }

        String userId = jwtService.extractUserId(currentToken);
        String rol = jwtService.extractRol(currentToken);
        String clinicaIdStr = jwtService.extractClinicaId(currentToken);
        UUID clinicaId = clinicaIdStr != null ? UUID.fromString(clinicaIdStr) : null;

        String newToken = jwtService.generateToken(UUID.fromString(userId), rol, clinicaId);
        return new AuthResponse(newToken);
    }

    public void logout(String currentToken) {
        Date expiration = jwtService.extractExpiration(currentToken);
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + currentToken, "1", Duration.ofMillis(ttlMillis));
        }
    }

    public List<MedicoInfoDto> getMedicosByClinicaId(UUID clinicaId) {
        return medicoRepository.findByClinicaId(clinicaId).stream()
                .map(m -> new MedicoInfoDto(
                        m.getId(),
                        m.getUsuario().getId(),
                        m.getNombres(),
                        m.getApellidos(),
                        m.getCmp(),
                        m.getEspecialidad(),
                        m.getUsuario().isActivo()
                ))
                .toList();
    }

    @Transactional
    public void setMedicoActivo(UUID clinicaId, UUID medicoId, boolean activo) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", medicoId.toString()));

        if (!medico.getClinicaId().equals(clinicaId)) {
            throw new TenantViolationException("El médico no pertenece a esta clínica");
        }

        Usuario usuario = medico.getUsuario();
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    public long countActiveMedicosByClinicaId(UUID clinicaId) {
        return medicoRepository.countActiveByClinicaId(clinicaId);
    }
}
