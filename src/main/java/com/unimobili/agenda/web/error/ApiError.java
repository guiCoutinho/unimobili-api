package com.unimobili.agenda.web.error;

import java.time.Instant;

/**
 * Corpo de erro padronizado da API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
