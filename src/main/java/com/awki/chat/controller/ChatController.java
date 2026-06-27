package com.awki.chat.controller;

import com.awki.auth.service.JwtService;
import com.awki.chat.dto.*;
import com.awki.chat.service.ChatService;
import com.awki.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final JwtService jwtService;

    @PostMapping("/mensaje")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<ChatMensajeResponse>> enviarMensaje(
            @Valid @RequestBody ChatMensajeRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        ChatMensajeResponse response = chatService.enviarMensaje(request, usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<ApiResponse<Page<MensajeChatResponse>>> obtenerHistorial(
            @RequestParam UUID embarazoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        Page<MensajeChatResponse> response = chatService.obtenerHistorial(embarazoId, page, size, usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/resumen-clinico/{embarazoId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN_CLINICA')")
    public ResponseEntity<ApiResponse<ResumenClinicoResponse>> obtenerResumenClinico(
            @PathVariable UUID embarazoId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        ResumenClinicoResponse response = chatService.obtenerResumenClinico(embarazoId, usuario);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/regenerar-resumen/{embarazoId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN_CLINICA')")
    public ResponseEntity<ApiResponse<Void>> regenerarResumen(
            @PathVariable UUID embarazoId,
            @RequestHeader("Authorization") String authHeader
    ) {
        UsuarioAutenticado usuario = getUsuarioAutenticado(authHeader);
        chatService.regenerarResumen(embarazoId, usuario);
        // Retornar 202 Accepted de forma inmediata sin bloquear
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(null));
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
