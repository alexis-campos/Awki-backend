package com.awki.sync.controller;

import com.awki.auth.service.JwtService;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.common.ApiResponse;
import com.awki.sync.dto.OfflineBatchRequest;
import com.awki.sync.dto.OfflineBatchResponse;
import com.awki.sync.dto.SyncEstadoResponse;
import com.awki.sync.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final JwtService jwtService;

    @PostMapping("/offline-batch")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<OfflineBatchResponse>> procesarBatch(
            @Valid @RequestBody OfflineBatchRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = extraerUsuario(authHeader);
        OfflineBatchResponse response = syncService.procesarBatch(request, usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/estado")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<SyncEstadoResponse>> obtenerEstado(
            @RequestParam String deviceId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = extraerUsuario(authHeader);
        SyncEstadoResponse response = syncService.obtenerEstado(deviceId, usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private UsuarioAutenticado extraerUsuario(String authHeader) {
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
