package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.UserBrandpayAuth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBrandpayAuthRepository extends JpaRepository<UserBrandpayAuth, Long> {
    UserBrandpayAuth findByUserId(Long userId);
    Optional<UserBrandpayAuth> findByCustomerKey(String customerKey);
    @Query(value = "SELECT * FROM user_brandpay_auth WHERE user_id = :userId", nativeQuery = true)
    Optional<UserBrandpayAuth> findByUserIdWithDeleted(@Param("userId") Long userId);
}
