package com.love.yourvoiceback.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    boolean existsByProviderAndProviderTransactionId(
            PaymentTransaction.PaymentProvider provider,
            String providerTransactionId
    );

    void deleteAllByOrderIdIn(Collection<Long> orderIds);
}
