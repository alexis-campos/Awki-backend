package com.awki.common;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiMeta meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, new ApiMeta());
    }

    public static <T> ApiResponse<T> error(String code, String message, int status) {
        return new ApiResponse<>(false, null, new ApiMeta(code, message, status));
    }
}
