package com.love.yourvoiceback.billing.dto.response;

import com.love.yourvoiceback.billing.PaymentOrder;

import java.time.LocalDateTime;

public record AdsRemovalOrderResponse(
        Long orderId,
        String productId,
        PaymentOrder.OrderStatus status,
        boolean adsFree,
        LocalDateTime confirmedAt
) {
    public static AdsRemovalOrderResponse from(PaymentOrder order, boolean adsFree) {
        return new AdsRemovalOrderResponse(
                order.getId(),
                order.getProductId(),
                order.getStatus(),
                adsFree,
                order.getConfirmedAt()
        );
    }
}
