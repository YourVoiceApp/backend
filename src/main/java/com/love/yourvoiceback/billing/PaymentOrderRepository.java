package com.love.yourvoiceback.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByIdAndUserId(Long id, Long userId);

    List<PaymentOrder> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
