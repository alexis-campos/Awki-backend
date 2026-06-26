package com.awki.auth.controller;

import com.awki.auth.dto.AuthResponse;
import com.awki.auth.dto.LoginRequest;
import com.awki.auth.dto.RegisterMedicoRequest;
import com.awki.auth.dto.RegisterPacienteRequest;
import com.awki.auth.service.AuthService;
import com.awki.auth.service.JwtService;
import com.awki.clinica.service.ClinicaService;
import com.awki.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ClinicaService clinicaService;
    private final JwtService jwtService;

    @PostMapping("/register/paciente")
    public ResponseEntity<ApiResponse<AuthResponse>> registerPaciente(
            @Valid @RequestBody RegisterPacienteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.registerPaciente(request)));
    }

    @PostMapping("/register/medico")
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    public ResponseEntity<ApiResponse<AuthResponse>> registerMedico(
            @Valid @RequestBody RegisterMedicoRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String clinicaIdStr = jwtService.extractClinicaId(token);
        if (clinicaIdStr != null) {
            clinicaService.verificarLimiteMedicos(UUID.fromString(clinicaIdStr));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.registerMedico(request, token)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(authHeader.substring(7))));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        authService.logout(authHeader.substring(7));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
