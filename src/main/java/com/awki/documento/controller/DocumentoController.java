package com.awki.documento.controller;

import com.awki.common.ApiResponse;
import com.awki.documento.dto.DocumentoDeleteResponse;
import com.awki.documento.dto.DocumentoResponse;
import com.awki.documento.dto.DocumentoUrlResponse;
import com.awki.documento.entity.TipoDocumento;
import com.awki.documento.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<DocumentoResponse>> upload(
            @RequestParam("embarazoId") UUID embarazoId,
            @RequestParam("tipoDocumento") TipoDocumento tipoDocumento,
            @RequestPart("archivo") MultipartFile archivo
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(documentoService.subirDocumento(embarazoId, tipoDocumento, archivo)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<Page<DocumentoResponse>>> listar(
            @RequestParam("embarazoId") UUID embarazoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(documentoService.listarDocumentos(embarazoId, page, size)));
    }

    @DeleteMapping("/{documentoId}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<DocumentoDeleteResponse>> eliminar(
            @PathVariable UUID documentoId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(documentoService.eliminarDocumento(documentoId)));
    }

    @GetMapping("/{documentoId}/url")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ApiResponse<DocumentoUrlResponse>> generarUrl(
            @PathVariable UUID documentoId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(documentoService.generarUrl(documentoId)));
    }
}