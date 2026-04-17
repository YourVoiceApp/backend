package com.love.yourvoiceback.billing;

import com.love.yourvoiceback.billing.dto.request.AdsRemovalConfirmRequest;
import com.love.yourvoiceback.billing.dto.response.AdsRemovalOrderResponse;
import com.love.yourvoiceback.billing.dto.response.AdsRemovalStatusResponse;
import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.user.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/billing/ads-removal")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/status")
    @Operation(summary = "현재 로그인한 사용자의 광고 제거 구매 상태를 조회합니다.")
    public ResponseEntity<AdsRemovalStatusResponse> getStatus(@CurrentUser User user) {
        return ResponseEntity.ok(billingService.getAdsRemovalStatus(user));
    }

    @PostMapping("/orders")
    @Operation(summary = "광고 제거용 결제 주문을 생성합니다.")
    public ResponseEntity<AdsRemovalOrderResponse> createOrder(@CurrentUser User user) {
        return ResponseEntity.ok(billingService.createAdsRemovalOrder(user));
    }

    @PostMapping("/confirm")
    @Operation(summary = "광고 제거 결제를 완료 처리합니다.")
    public ResponseEntity<AdsRemovalOrderResponse> confirm(
            @CurrentUser User user,
            @Valid @RequestBody AdsRemovalConfirmRequest request
    ) {
        return ResponseEntity.ok(billingService.confirmAdsRemoval(user, request));
    }
}
