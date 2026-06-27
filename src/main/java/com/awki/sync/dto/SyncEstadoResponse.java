package com.awki.sync.dto;

import java.time.LocalDateTime;

public record SyncEstadoResponse(
        String deviceId,
        LocalDateTime ultimaSincronizacion
) {}
