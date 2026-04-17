package com.love.yourvoiceback.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    Optional<Entitlement> findByUserIdAndCode(Long userId, Entitlement.EntitlementCode code);

    boolean existsByUserIdAndCodeAndActiveTrue(Long userId, Entitlement.EntitlementCode code);

    void deleteAllByUserId(Long userId);
}
