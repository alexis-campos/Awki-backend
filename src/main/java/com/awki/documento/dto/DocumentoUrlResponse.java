package com.awki.documento.dto;

import java.time.LocalDateTime;

public record DocumentoUrlResponse(
        String url,
        LocalDateTime expiraAt
) {}