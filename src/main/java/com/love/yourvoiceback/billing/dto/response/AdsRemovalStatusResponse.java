package com.love.yourvoiceback.billing.dto.response;

import java.time.LocalDateTime;

public record AdsRemovalStatusResponse(
        boolean adsFree,
        String productId,
        LocalDateTime purchasedAt
) {
    public static AdsRemovalStatusResponse of(boolean adsFree, String productId, LocalDateTime purchasedAt) {
        return new AdsRemovalStatusResponse(adsFree, productId, purchasedAt);
    }
}
