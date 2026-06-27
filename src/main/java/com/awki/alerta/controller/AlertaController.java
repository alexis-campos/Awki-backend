package com.awki.alerta.controller;

import com.awki.alerta.dto.*;
import com.awki.alerta.service.AlertaService;
import com.awki.auth.service.JwtService;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;
    private final JwtService jwtService;

    @PostMapping("/sos")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<SosResponse>> crearSos(
            @Valid @RequestBody SosRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        SosResponse response = alertaService.crearSos(request, usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN_CLINICA')")
    public ResponseEntity<ApiResponse<Page<AlertaResponse>>> listarAlertas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertaResponse> response = alertaService.listarAlertas(usuario, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/no-leidas/count")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<CountNoLeidasResponse>> contarNoLeidas(
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        CountNoLeidasResponse response = alertaService.contarNoLeidas(usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{alertaId}/marcar-leida")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<Void>> marcarLeida(
            @PathVariable UUID alertaId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        alertaService.marcarLeida(alertaId, usuario);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/dispositivos/registrar")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<Void>> registrarFcmToken(
            @Valid @RequestBody RegistrarTokenRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        alertaService.registrarFcmToken(request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    private UsuarioAutenticado getUsuarioAutenticado(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Header Authorization no válido");
        }
        String token = authHeader.substring(7);
        UUID userId = UUID.fromString(jwtService.extractUserId(token));
        String rol = jwtService.extractRol(token);
        String clinicaIdStr = jwtService.extractClinicaId(token);
        UUID clinicaId = clinicaIdStr != null ? UUID.fromString(clinicaIdStr) : null;
        return new UsuarioAutenticado(userId, rol, clinicaId);
    }
}
