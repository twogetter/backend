package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findAllByUserBrandpayAuth_UserId(Long userBrandpayAuthUserId);

    Optional<PaymentMethod> findByIdAndUserBrandpayAuth_UserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.type = 'NORMAL' WHERE pm.userBrandpayAuth.customerKey = :customerKey")
    void updateTypeToNormalByCustomerKey(String customerKey);
}
