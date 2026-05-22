package com.smartcart.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public BusinessException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static BusinessException notFound(String resource) {
        return new BusinessException(resource + " not found", "NOT_FOUND", 404);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(message, "BAD_REQUEST", 400);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, "UNAUTHORIZED", 401);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, "FORBIDDEN", 403);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(message, "CONFLICT", 409);
    }
}
