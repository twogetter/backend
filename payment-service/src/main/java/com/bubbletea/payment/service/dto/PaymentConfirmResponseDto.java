package com.bubbletea.payment.service.dto;

public record PaymentConfirmResponseDto(
        String paymentKey,
        String orderId,
        String orderName,
        String status,

        // 2. 이력 및 일시
        String requestedAt,
        String approvedAt,

        // 3. 금액 정보
        Long totalAmount,
        Long balanceAmount,

        // 4. 결제 수단별 상세 정보 (선택적 구현)
        CardDto card,
        TransferDto transfer,

        String type
) {
    public record CardDto(
            Long amount,
            String issuerCode,
            String acquirerCode,
            String number,
            Integer installmentPlanMonths,
            String approveNo
    ) {}

    public record TransferDto(
            String bankCode,
            String settlementStatus
    ) {}

}
