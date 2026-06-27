package com.awki.vinculacion.controller;

import com.awki.common.ApiResponse;
import com.awki.vinculacion.dto.GenerarCodigoResponse;
import com.awki.vinculacion.dto.UsarCodigoRequest;
import com.awki.vinculacion.dto.VinculoResponse;
import com.awki.vinculacion.service.VinculacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vinculacion")
@RequiredArgsConstructor
public class VinculacionController {

    private final VinculacionService vinculacionService;

    @PostMapping("/generar-codigo")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<GenerarCodigoResponse>> generarCodigo() {
        return ResponseEntity.ok(ApiResponse.ok(vinculacionService.generarCodigo()));
    }

    @PostMapping("/usar-codigo")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<VinculoResponse>> usarCodigo(
            @Valid @RequestBody UsarCodigoRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(vinculacionService.usarCodigo(request)));
    }

    @GetMapping("/mis-vinculos")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN_CLINICA')")
    public ResponseEntity<ApiResponse<List<VinculoResponse>>> misVinculos() {
        return ResponseEntity.ok(ApiResponse.ok(vinculacionService.misVinculos()));
    }

    @DeleteMapping("/{vinculoId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN_CLINICA')")
    public ResponseEntity<ApiResponse<VinculoResponse>> finalizarVinculo(
            @PathVariable UUID vinculoId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(vinculacionService.finalizarVinculo(vinculoId)));
    }
}