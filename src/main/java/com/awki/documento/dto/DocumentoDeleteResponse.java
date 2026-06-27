package com.awki.documento.dto;

import java.time.LocalDateTime;

public record DocumentoDeleteResponse(
        String mensaje,
        LocalDateTime eliminadoAt
) {}