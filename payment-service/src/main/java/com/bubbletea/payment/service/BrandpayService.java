package com.bubbletea.payment.service;

import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.entity.enums.PaymentMethodStatus;
import com.bubbletea.payment.entity.enums.PaymentMethodType;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandpayService {
    private static final Logger logger = LoggerFactory.getLogger(BrandpayService.class);

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserBrandpayAuthRepository userBrandpayAuthRepository;
    private final TossBrandpayApiClient tossBrandpayApiClient;

    @Transactional
    public BrandpayAuthDetailResponseDto getCustomerKey(Long userId) {
        UserBrandpayAuth auth = userBrandpayAuthRepository.findByUserId(userId);
        if (auth == null) {
            auth = userBrandpayAuthRepository.save(UserBrandpayAuth.builder()
                    .userId(userId)
                    .customerKey(userId + "_" + UUID.randomUUID().toString())
                    .build());
        }

        return new BrandpayAuthDetailResponseDto(auth.getUserId(), auth.getCustomerKey());
    }

    @Transactional
    public ConnectBrandpayResponseDto connectBrandpay(ConnectBrandpayRequestDto request) {
        logger.info("Connecting Brandpay for user: {}, customerKey: {}", request.userId(), request.customerKey());

        try {
            TossAccessTokenResponseDto tokenResponse = tossBrandpayApiClient.getAccessToken(request.customerKey(), request.code());

            if (tokenResponse == null || tokenResponse.error() != null) {
                logger.error("Toss API Error: {}", tokenResponse);
                throw new PaymentException(PaymentErrorCode.TOSS_API_ERROR,
                        tokenResponse == null ? "토스 API 오류" : tokenResponse.message());
            }

            String accessToken = tokenResponse.accessToken();
            String refreshToken = tokenResponse.refreshToken();
            if (accessToken == null || accessToken.isEmpty()) {
                throw new PaymentException(PaymentErrorCode.TOSS_API_ERROR, "Access Token을 받지 못했습니다");
            }

            UserBrandpayAuth existingUserBrandpayAuth = userBrandpayAuthRepository
                    .findByUserIdWithDeleted(request.userId()).orElse(null);

            UserBrandpayAuth userBrandpayAuth;
            if (existingUserBrandpayAuth != null) {
                logger.info("Updating existing Brandpay token for user: {}", request.userId());
                existingUserBrandpayAuth.updateTossTokens(accessToken, refreshToken);
//                existingUserBrandpayAuth.setStatus(PaymentMethodStatus.ACTIVE);
                userBrandpayAuth = existingUserBrandpayAuth;
            } else {
                logger.info("Creating new Brandpay token for user: {}", request.userId());
                userBrandpayAuth = UserBrandpayAuth.builder()
                        .userId(request.userId())
                        .customerKey(request.customerKey())
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            }

            UserBrandpayAuth savedUserBrandpayAuth = userBrandpayAuthRepository.save(userBrandpayAuth);
            logger.info("Successfully saved/updated payment method: userId={}", savedUserBrandpayAuth.getUserId());

            return ConnectBrandpayResponseDto.of(savedUserBrandpayAuth);

        } catch (PaymentException e) {
            logger.error("PaymentException in connectBrandpay", e);
            throw e;
        } catch (Exception e) {
            logger.error("Exception in connectBrandpay", e);
            throw new PaymentException(PaymentErrorCode.TOSS_API_ERROR, "토스 연동 중 오류가 발생했습니다");
        }
    }

    @Transactional
    public void syncPaymentMethods(UserBrandpayAuth userBrandpayAuth) throws Exception {
        TossRegisteredPaymentMethodsResponseDto methodResponse = tossBrandpayApiClient.getRegisteredPaymentMethods(userBrandpayAuth.getAccessToken());

        if (methodResponse == null || methodResponse.error() != null) {
            logger.error("Failed to load Brandpay payment methods: {}", methodResponse);
            throw new PaymentException(PaymentErrorCode.TOSS_API_ERROR,
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
                .collect(Collectors.toMap(PaymentMethod::getTossMethodId, method -> method));

        List<PaymentMethod> toSave = new ArrayList<>();
        List<PaymentMethod> toDelete = new ArrayList<>();

        for (PaymentMethod incoming : incomingMethods) {
            PaymentMethod existing = existingMethodMap.get(incoming.getTossMethodId());

            if (existing != null) {
                existing.updateFrom(incoming);
                toSave.add(existing);

                existingMethodMap.remove(incoming.getTossMethodId());
            } else {
                toSave.add(incoming);
            }
        }

        toDelete.addAll(existingMethodMap.values());

        if (!toDelete.isEmpty()) {
            paymentMethodRepository.deleteAll(toDelete);
            logger.info("Deleted Brandpay payment methods for userId={}, count={}", userBrandpayAuth.getUserId(), toDelete.size());
        }

        if (!toSave.isEmpty()) {
            paymentMethodRepository.saveAll(toSave);
            logger.info("Saved/Updated Brandpay payment methods for userId={}, count={}", userBrandpayAuth.getUserId(), toSave.size());
        }
    }

    @Transactional
    public void disconnectBrandpayByWebhook(String customerKey) {
        UserBrandpayAuth auth = userBrandpayAuthRepository.findByCustomerKey(customerKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 인증 정보입니다."));

        userBrandpayAuthRepository.delete(auth);
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
                    .tossMethodId(methodKey)
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
                    .tossMethodId(methodKey)
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