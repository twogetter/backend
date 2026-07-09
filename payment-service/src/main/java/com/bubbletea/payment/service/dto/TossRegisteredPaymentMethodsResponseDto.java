package com.bubbletea.payment.service.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record TossRegisteredPaymentMethodsResponseDto(
    List<Card> cards,
    List<Account> accounts,
    String selectedMethodId,
    String error,
    String message
) {

    public record Card (
            String methodKey,
            String provider,
            String cardName,
            String cardNumber,
            String status
    ) {
    }

    public record Account(
            String methodKey,
            String bank,
            String accountName,
            String accountNumber,
            String status
    ) {
    }
}
