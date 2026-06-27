package com.awki.control.controller;

import com.awki.common.ApiResponse;
import com.awki.control.dto.ControlPrenatalRequest;
import com.awki.control.dto.ControlPrenatalResponse;
import com.awki.control.dto.ControlPrenatalResumenResponse;
import com.awki.control.service.ControlPrenatalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/controles")
@RequiredArgsConstructor
public class ControlPrenatalController {

    private final ControlPrenatalService controlPrenatalService;

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ApiResponse<ControlPrenatalResponse>> crearControl(
            @Valid @RequestBody ControlPrenatalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(controlPrenatalService.crearControl(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<List<ControlPrenatalResumenResponse>>> listarPorEmbarazo(
            @RequestParam("embarazo_id") UUID embarazoId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(controlPrenatalService.listarPorEmbarazo(embarazoId)));
    }
}
