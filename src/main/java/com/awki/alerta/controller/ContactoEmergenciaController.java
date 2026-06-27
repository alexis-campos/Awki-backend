package com.awki.alerta.controller;

import com.awki.alerta.dto.ContactoEmergenciaRequest;
import com.awki.alerta.dto.ContactoEmergenciaResponse;
import com.awki.alerta.service.ContactoEmergenciaService;
import com.awki.auth.service.JwtService;
import com.awki.chat.dto.UsuarioAutenticado;
import com.awki.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contactos-emergencia")
@RequiredArgsConstructor
public class ContactoEmergenciaController {

    private final ContactoEmergenciaService contactoService;
    private final JwtService jwtService;

    @GetMapping
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<List<ContactoEmergenciaResponse>>> listarContactos(
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        List<ContactoEmergenciaResponse> response = contactoService.listarContactos(usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<ContactoEmergenciaResponse>> agregarContacto(
            @Valid @RequestBody ContactoEmergenciaRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        ContactoEmergenciaResponse response = contactoService.agregarContacto(request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<Void>> eliminarContacto(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        contactoService.eliminarContacto(id, usuario);
        return ResponseEntity.ok(ApiResponse.ok(null));
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
