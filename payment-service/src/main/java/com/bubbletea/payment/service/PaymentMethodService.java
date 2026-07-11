package com.bubbletea.payment.service;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserBrandpayAuthRepository userBrandpayAuthRepository;

    @Transactional
    public void updatePaymentMethodBilling(Long paymentMethodId, Long userid) {
        UserBrandpayAuth auth = userBrandpayAuthRepository.findByUserId(userid)
                .orElseThrow(() -> new AppException(PaymentErrorCode.USER_BRANDPAY_AUTH_NOT_FOUND));

        if(!auth.isBillingAgreed()) {
            throw new AppException(PaymentErrorCode.UNAUTHORIZED_ACCESS, "정기결제 약관에 동의가 필요합니다.");
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserBrandpayAuth_UserId(paymentMethodId, userid)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_METHOD_NOT_FOUND));

        paymentMethod.registerBilling();
    }

}
