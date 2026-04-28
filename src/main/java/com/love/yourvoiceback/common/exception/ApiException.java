package com.love.yourvoiceback.common.exception;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    private ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static ApiException error(ErrorCode errorCode) {
        return new ApiException(errorCode, errorCode.message(), null);
    }

    public static ApiException error(ErrorCode errorCode, Throwable cause) {
        return new ApiException(errorCode, errorCode.message(), cause);
    }

    public static ApiException error(ErrorCode errorCode, String message) {
        return new ApiException(errorCode, message, null);
    }

    public static ApiException error(ErrorCode errorCode, String message, Throwable cause) {
        return new ApiException(errorCode, message, cause);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
