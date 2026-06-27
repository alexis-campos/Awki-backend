package com.awki.documento.dto;

import com.awki.documento.entity.SubidoPor;
import com.awki.documento.entity.TipoDocumento;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoResponse(
        UUID id,
        UUID embarazoId,
        SubidoPor subidoPor,
        UUID subidoPorId,
        TipoDocumento tipoDocumento,
        String nombreArchivo,
        String contentType,
        Long tamanoBytes,
        LocalDateTime createdAt
) {}