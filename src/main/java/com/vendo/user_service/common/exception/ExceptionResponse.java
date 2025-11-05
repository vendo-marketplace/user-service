package com.vendo.user_service.common.exception;

import lombok.Builder;

// TODO move to common
@Builder
public record ExceptionResponse(
        String message,
        String type,
        int code,
        String path) {
}
