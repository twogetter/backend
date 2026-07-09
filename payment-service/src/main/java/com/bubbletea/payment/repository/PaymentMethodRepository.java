package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.PaymentMethod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findAllByUserBrandpayAuth_UserId(Long userBrandpayAuthUserId);
}
