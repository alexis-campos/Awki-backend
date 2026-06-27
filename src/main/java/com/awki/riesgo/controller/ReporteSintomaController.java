package com.awki.riesgo.controller;

import com.awki.common.ApiResponse;
import com.awki.riesgo.dto.ReporteSintomaRequest;
import com.awki.riesgo.dto.ReporteSintomaResponse;
import com.awki.riesgo.service.ReporteSintomaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/riesgo/reportes-diarios")
@RequiredArgsConstructor
public class ReporteSintomaController {

    private final ReporteSintomaService service;

    @PostMapping
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<ReporteSintomaResponse>> registrar(
            @Valid @RequestBody ReporteSintomaRequest request) {
        ReporteSintomaResponse res = service.registrarReporte(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(res));
    }

    @GetMapping("/{embarazoId}")
    public ResponseEntity<ApiResponse<List<ReporteSintomaResponse>>> listar(
            @PathVariable UUID embarazoId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listarHistorial(embarazoId)));
    }
}
