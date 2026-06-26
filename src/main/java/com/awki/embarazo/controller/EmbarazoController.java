package com.awki.embarazo.controller;

import com.awki.auth.service.JwtService;
import com.awki.common.ApiResponse;
import com.awki.embarazo.dto.*;
import com.awki.embarazo.service.EmbarazoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/embarazos")
@RequiredArgsConstructor
public class EmbarazoController {

    private final EmbarazoService embarazoService;
    private final JwtService jwtService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<EmbarazoResponse>> crearEmbarazo(
            @Valid @RequestBody EmbarazoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(embarazoService.crearEmbarazo(request)));
    }

    @GetMapping("/activo")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<EmbarazoResponse>> getEmbarazoActivo(
            @RequestParam(required = false) UUID pacienteId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String rol = jwtService.extractRol(token);

        UUID targetPacienteId;
        if ("PACIENTE".equals(rol)) {
            // El paciente consulta su propio embarazo activo
            targetPacienteId = UUID.fromString(jwtService.extractUserId(token));
        } else {
            // El médico consulta el embarazo activo de una paciente vinculada
            if (pacienteId == null) {
                throw new IllegalArgumentException("El parámetro pacienteId es requerido para médicos");
            }
            targetPacienteId = pacienteId;
            // TODO: MODULO_VINCULACION - Validar vinculación médico-paciente para multi-tenant
        }

        return ResponseEntity.ok(ApiResponse.ok(embarazoService.obtenerEmbarazoActivo(targetPacienteId)));
    }

    @GetMapping("/{embarazoId}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<EmbarazoResponse>> getEmbarazoPorId(
            @PathVariable UUID embarazoId
    ) {
        // TODO: MODULO_VINCULACION - Validar permisos de acceso multi-tenant
        return ResponseEntity.ok(ApiResponse.ok(embarazoService.obtenerEmbarazoPorId(embarazoId)));
    }

    @PutMapping("/{embarazoId}/antecedentes")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<AntecedentesResponse>> actualizarAntecedentes(
            @PathVariable UUID embarazoId,
            @RequestBody AntecedentesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(embarazoService.crearOActualizarAntecedentes(embarazoId, request)));
    }

    @GetMapping("/{embarazoId}/antecedentes")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<AntecedentesResponse>> getAntecedentes(
            @PathVariable UUID embarazoId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(embarazoService.obtenerAntecedentes(embarazoId)));
    }

    @PatchMapping("/{embarazoId}/finalizar")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<EmbarazoResponse>> finalizarEmbarazo(
            @PathVariable UUID embarazoId,
            @Valid @RequestBody FinalizarEmbarazoRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(embarazoService.finalizarEmbarazo(embarazoId, request)));
    }
}
