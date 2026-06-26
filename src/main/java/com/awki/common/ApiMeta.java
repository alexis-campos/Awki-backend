package com.awki.common;

import java.time.LocalDateTime;

public record ApiMeta(
        String code,
        String message,
        int status,
        LocalDateTime timestamp
) {
    public ApiMeta() {
        this(null, null, 200, LocalDateTime.now());
    }

    public ApiMeta(String code, String message, int status) {
        this(code, message, status, LocalDateTime.now());
    }
}
