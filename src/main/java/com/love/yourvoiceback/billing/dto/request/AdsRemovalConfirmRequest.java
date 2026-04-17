package com.love.yourvoiceback.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdsRemovalConfirmRequest(
        @NotNull Long orderId,
        @NotBlank String purchaseToken
) {
}
