package com.awki.sync.dto;

public record OfflineBatchResponse(
        int procesados,
        int descartados,
        int conflictos,
        int alertasGeneradasRetroactivamente
) {}
