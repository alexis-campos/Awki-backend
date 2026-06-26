package com.awki.clinica.controller;

import com.awki.auth.service.JwtService;
import com.awki.clinica.dto.*;
import com.awki.clinica.service.ClinicaService;
import com.awki.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinica")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_CLINICA')")
public class ClinicaController {

    private final ClinicaService clinicaService;
    private final JwtService jwtService;

    @GetMapping("/mi-clinica")
    public ResponseEntity<ApiResponse<ClinicaResponse>> getMiClinica(
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID clinicaId = getClinicaIdFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.ok(clinicaService.getClinicaById(clinicaId)));
    }

    @GetMapping("/medicos")
    public ResponseEntity<ApiResponse<List<ClinicaMedicoResponse>>> getMedicos(
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID clinicaId = getClinicaIdFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.ok(clinicaService.getMedicosByClinica(clinicaId)));
    }

    @PatchMapping("/medicos/{medicoId}/estado")
    public ResponseEntity<ApiResponse<Void>> cambiarEstadoMedico(
            @PathVariable UUID medicoId,
            @RequestParam boolean activo,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID clinicaId = getClinicaIdFromHeader(authHeader);
        clinicaService.cambiarEstadoMedico(clinicaId, medicoId, activo);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/metricas")
    public ResponseEntity<ApiResponse<ClinicaMetricasResponse>> getMetricas(
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID clinicaId = getClinicaIdFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.ok(clinicaService.getMetricas(clinicaId)));
    }

    @GetMapping("/pacientes-resumen")
    public ResponseEntity<ApiResponse<List<PacienteResumenResponse>>> getPacientesResumen(
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID clinicaId = getClinicaIdFromHeader(authHeader);
        return ResponseEntity.ok(ApiResponse.ok(clinicaService.getPacientesResumen(clinicaId)));
    }

    private UUID getClinicaIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Header Authorization no válido");
        }
        String token = authHeader.substring(7);
        String clinicaIdStr = jwtService.extractClinicaId(token);
        if (clinicaIdStr == null) {
            throw new IllegalArgumentException("No se encontró clinicaId en el token");
        }
        return UUID.fromString(clinicaIdStr);
    }
}
