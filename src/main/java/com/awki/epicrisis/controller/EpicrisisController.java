package com.awki.epicrisis.controller;

import com.awki.auth.service.JwtService;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.common.ApiResponse;
import com.awki.epicrisis.dto.EpicrisisGenerarRequest;
import com.awki.epicrisis.dto.EpicrisisJobResponse;
import com.awki.epicrisis.dto.EpicrisisResponse;
import com.awki.epicrisis.service.EpicrisisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/epicrisis")
@RequiredArgsConstructor
public class EpicrisisController {

    private final EpicrisisService epicrisisService;
    private final JwtService jwtService;

    @PostMapping("/generar")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<EpicrisisJobResponse>> generarEpicrisis(
            @Valid @RequestBody EpicrisisGenerarRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        EpicrisisJobResponse response = epicrisisService.iniciarGeneracion(request, usuario);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(response));
    }

    @GetMapping("/estado/{jobId}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<EpicrisisJobResponse>> obtenerEstadoJob(
            @PathVariable UUID jobId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        EpicrisisJobResponse response = epicrisisService.obtenerEstadoJob(jobId, usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{epicrisisId}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<EpicrisisResponse>> obtenerEpicrisis(
            @PathVariable UUID epicrisisId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        EpicrisisResponse response = epicrisisService.obtenerEpicrisis(epicrisisId, usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping(value = "/{epicrisisId}/descargar", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable UUID epicrisisId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        byte[] pdfBytes = epicrisisService.descargarPdf(epicrisisId, usuario);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "epicrisis_" + epicrisisId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
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
