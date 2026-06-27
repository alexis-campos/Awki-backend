package com.awki.sync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OfflineBatchRequest(
        @NotBlank @Size(max = 255) String deviceId,
        @NotNull @Valid List<OfflineMensajeItem> items
) {}
