package com.bubbletea.payment.service;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.entity.enums.PaymentMethodStatus;
import com.bubbletea.payment.entity.enums.PaymentMethodType;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import com.bubbletea.payment.service.dto.BrandpayAuthDetailResponseDto;
import com.bubbletea.payment.service.dto.ConnectBrandpayRequestDto;
import com.bubbletea.payment.service.dto.ConnectBrandpayResponseDto;
import com.bubbletea.payment.service.dto.PaymentMethodResponseDto;
import com.bubbletea.payment.service.external.TossBrandpayApiClient;
import com.bubbletea.payment.service.dto.TossAccessTokenResponseDto;
import com.bubbletea.payment.service.dto.TossRegisteredPaymentMethodsResponseDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandpayService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserBrandpayAuthRepository userBrandpayAuthRepository;
    private final TossBrandpayApiClient tossBrandpayApiClient;

    @Transactional
    public BrandpayAuthDetailResponseDto getCustomerKey(Long userId) {
        //TODO: 원래는 userId를 헤더로 받아서 검증해야하지만 일단 인증 없이 진행
        UserBrandpayAuth auth = userBrandpayAuthRepository.findByUserIdWithDeleted(userId).orElse(null);
        if (auth == null) {
            auth = userBrandpayAuthRepository.save(UserBrandpayAuth.builder()
                    .userId(userId)
                    .customerKey(userId + "_" + UUID.randomUUID().toString())
                    .build());
        }

        List<BrandpayAuthDetailResponseDto.PaymentMethodDto> paymentMethods = paymentMethodRepository.findAllByUserBrandpayAuth_UserId(auth.getUserId()).stream()
                .map(BrandpayAuthDetailResponseDto.PaymentMethodDto::of)
                .toList();

        return new BrandpayAuthDetailResponseDto(
                auth.getUserId(),
                auth.getCustomerKey(),
                paymentMethods,
                auth.isBillingAgreed(),
                null,
                null
        );
    }

    @Transactional
    public ConnectBrandpayResponseDto connectBrandpay(ConnectBrandpayRequestDto request) {
        log.info("Connecting Brandpay for user: {}, customerKey: {}", request.userId(), request.customerKey());

        try {
            TossAccessTokenResponseDto tokenResponse = tossBrandpayApiClient.getAccessToken(request.customerKey(), request.code());

            if (tokenResponse == null || tokenResponse.error() != null) {
                log.error("Toss API Error: {}", tokenResponse);
                throw new PaymentSystemException(PaymentErrorCode.TOSS_PAYMENT_REJECTED,
                        tokenResponse == null ? "토스 API 오류" : tokenResponse.message());
            }

            String accessToken = tokenResponse.accessToken();
            String refreshToken = tokenResponse.refreshToken();
            if (accessToken == null || accessToken.isEmpty()) {
                throw new PaymentSystemException(PaymentErrorCode.TOSS_PAYMENT_REJECTED, "Access Token을 받지 못했습니다");
            }

            UserBrandpayAuth existingUserBrandpayAuth = userBrandpayAuthRepository
                    .findByUserIdWithDeleted(request.userId()).orElse(null);

            UserBrandpayAuth userBrandpayAuth;
            if (existingUserBrandpayAuth != null) {
                log.info("Updating existing Brandpay token for user: {}", request.userId());
                existingUserBrandpayAuth.updateTossTokens(accessToken, refreshToken);
                userBrandpayAuth = existingUserBrandpayAuth;
            } else {
                userBrandpayAuth = UserBrandpayAuth.builder()
                        .userId(request.userId())
                        .customerKey(request.customerKey())
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            }

            UserBrandpayAuth savedUserBrandpayAuth = userBrandpayAuthRepository.save(userBrandpayAuth);

            return ConnectBrandpayResponseDto.of(savedUserBrandpayAuth);
        }
        catch (PaymentSystemException e) {
            log.error("PaymentException in connectBrandpay", e);
            throw e;
        }
    }

    @Transactional
    public void syncPaymentMethods(String customerKey) throws Exception {

        UserBrandpayAuth userBrandpayAuth = userBrandpayAuthRepository.findByCustomerKey(customerKey).orElseThrow();

        TossRegisteredPaymentMethodsResponseDto methodResponse = tossBrandpayApiClient.getRegisteredPaymentMethods(userBrandpayAuth.getAccessToken());

        if (methodResponse == null || methodResponse.error() != null) {
            log.error("Failed to load Brandpay payment methods: {}", methodResponse);
            throw new PaymentSystemException(PaymentErrorCode.TOSS_PAYMENT_REJECTED,
                    methodResponse == null ? "등록된 결제수단을 조회하지 못했습니다" : methodResponse.message());
        }

        List<TossRegisteredPaymentMethodsResponseDto.Card> cards = methodResponse.cards();
        List<TossRegisteredPaymentMethodsResponseDto.Account> accounts = methodResponse.accounts();
        String selectedMethodId = methodResponse.selectedMethodId();

        // 1. 토스 API 응답으로부터 저장할 임시 객체 리스트 생성
        List<PaymentMethod> incomingMethods = new ArrayList<>();
        if (cards != null) incomingMethods.addAll(mapCardMethods(userBrandpayAuth, cards, selectedMethodId));
        if (accounts != null) incomingMethods.addAll(mapAccountMethods(userBrandpayAuth, accounts, selectedMethodId));

        List<PaymentMethod> existingMethods = paymentMethodRepository.findAllByUserBrandpayAuth_UserId(userBrandpayAuth.getUserId());

        Map<String, PaymentMethod> existingMethodMap = existingMethods.stream()
                .collect(Collectors.toMap(PaymentMethod::getTossMethodKey, method -> method));

        List<PaymentMethod> toSave = new ArrayList<>();
        List<PaymentMethod> toDelete = new ArrayList<>();

        for (PaymentMethod incoming : incomingMethods) {
            PaymentMethod existing = existingMethodMap.get(incoming.getTossMethodKey());

            if (existing != null) {
                existing.updateFrom(incoming);
                toSave.add(existing);

                existingMethodMap.remove(incoming.getTossMethodKey());
            } else {
                toSave.add(incoming);
            }
        }

        toDelete.addAll(existingMethodMap.values());

        if (!toDelete.isEmpty()) {
            paymentMethodRepository.deleteAll(toDelete);
            log.info("Deleted Brandpay payment methods for userId={}, count={}", userBrandpayAuth.getUserId(), toDelete.size());
        }

        if (!toSave.isEmpty()) {
            paymentMethodRepository.saveAll(toSave);
            log.info("Saved/Updated Brandpay payment methods for userId={}, count={}", userBrandpayAuth.getUserId(), toSave.size());
        }
    }

    @Transactional
    public void disconnectBrandpayByWebhook(String customerKey) {
        UserBrandpayAuth auth = userBrandpayAuthRepository.findByCustomerKey(customerKey)
                .orElseThrow(() -> new AppException(PaymentErrorCode.USER_NOT_FOUND, "존재하지 않는 인증 정보입니다."));

        userBrandpayAuthRepository.delete(auth);
    }

    @Transactional
    public void billingAllow(String customerKey) {
        UserBrandpayAuth auth = userBrandpayAuthRepository.findByCustomerKey(customerKey)
                .orElseThrow(() -> new AppException(PaymentErrorCode.USER_NOT_FOUND, "존재하지 않는 인증 정보입니다."));

        auth.agreeBilling();
        userBrandpayAuthRepository.save(auth);
    }

    @Transactional
    public void terminateBilling(String customerKey) {

        UserBrandpayAuth auth = userBrandpayAuthRepository.findByCustomerKey(customerKey)
                .orElseThrow(() -> new AppException(PaymentErrorCode.USER_NOT_FOUND, "가입자를 찾을 수 없습니다."));

        auth.terminateBilling();

        paymentMethodRepository.updateTypeToNormalByCustomerKey(customerKey);
    }

    public List<PaymentMethodResponseDto> getPaymentMethods(Long userId) {
        return paymentMethodRepository.findAllByUserBrandpayAuth_UserId(userId)
                .stream()
                .map(pm -> PaymentMethodResponseDto.builder()
                        .id(pm.getId())
                        .provider(pm.getProvider())
                        .type(pm.getType())
                        .displayName(pm.getDisplayName())
                        .maskedNumber(pm.getMaskedNumber())
                        .isDefault(pm.getIsDefault())
                        .status(pm.getStatus())
                        .build())
                .toList();
    }

    private List<PaymentMethod> mapCardMethods(UserBrandpayAuth userBrandpayAuth, List<TossRegisteredPaymentMethodsResponseDto.Card> cards, String selectedMethodId) {
        List<PaymentMethod> methods = new ArrayList<>();
        for (TossRegisteredPaymentMethodsResponseDto.Card card : cards) {
            if (card == null) continue;

            String methodKey = card.methodKey();
            if (methodKey == null || methodKey.isBlank()) {
                continue;
            }

            methods.add(PaymentMethod.builder()
                    .userBrandpayAuth(userBrandpayAuth)
                    .tossMethodId(card.id())
                    .tossMethodKey(methodKey)
                    .displayName(card.cardName())
                    .maskedNumber(card.cardNumber())
                    .type(PaymentMethodType.NORMAL)
                    .status(mapStatus(card.status()))
                    .isDefault(methodKey.equals(selectedMethodId))
                    .build());

        }
        return methods;
    }

    private List<PaymentMethod> mapAccountMethods(UserBrandpayAuth userBrandpayAuth, List<TossRegisteredPaymentMethodsResponseDto.Account> accounts, String selectedMethodId) {
        List<PaymentMethod> methods = new ArrayList<>();
        for (TossRegisteredPaymentMethodsResponseDto.Account account : accounts) {
            if (account == null) continue;

            String methodKey = account.methodKey();
            if (methodKey == null || methodKey.isBlank()) {
                continue;
            }

            methods.add(PaymentMethod.builder()
                    .userBrandpayAuth(userBrandpayAuth)
                    .tossMethodId(account.id())
                    .tossMethodKey(methodKey)
                    .displayName(account.accountName())
                    .maskedNumber(account.accountNumber())
                    .type(PaymentMethodType.NORMAL)
                    .status(mapStatus(account.status()))
                    .isDefault(methodKey.equals(selectedMethodId))
                    .build());
        }
        return methods;
    }

    private PaymentMethodStatus mapStatus(String status) {
        if ("ENABLED".equals(status)) {
            return PaymentMethodStatus.ACTIVE;
        }
        if ("DISABLED".equals(status)) {
            return PaymentMethodStatus.INACTIVE;
        }
        return PaymentMethodStatus.UNUSABLE;
    }
}