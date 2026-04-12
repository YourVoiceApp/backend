package com.love.yourvoiceback.common.dto;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;

public record ApiErrorResponse(
        String code,
        String message
) {
    public static ApiErrorResponse from(ApiException exception) {
        return new ApiErrorResponse(
                exception.getErrorCode().name(),
                exception.getMessage()
        );
    }

    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(
                errorCode.name(),
                errorCode.message()
        );
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message) {
        return new ApiErrorResponse(
                errorCode.name(),
                message
        );
    }
}
