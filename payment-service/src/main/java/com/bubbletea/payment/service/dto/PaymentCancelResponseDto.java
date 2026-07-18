package com.bubbletea.payment.service.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PaymentCancelResponseDto(
        String paymentKey,
        String orderId,
        String orderName,
        String status, // 💡 여기에 "CANCELLED"가 들어옵니다.

        // 💡 토스는 취소 이력을 리스트 형태로 넣어줍니다. (부분 취소가 가능하기 때문)
        List<CancelDetailDto> cancels
) {
        // 내부 정적 팩토리 메서드나 게터를 만들어 첫 번째 취소 건의 상세 내역을 편하게 꺼내 쓰도록 유틸 추가 가능
        public CancelDetailDto getLatestCancel() {
                if (cancels != null && !cancels.isEmpty()) {
                        return cancels.getLast(); // 가장 최근 취소 정보 반환
                }
                return null;
        }

        public record CancelDetailDto(
                String cancellationKey, // 토스 고유의 취소 키
                Long cancelAmount,      // 취소된 금액
                String cancelReason,    // 취소 사유
                String canceledAt,      // 취소된 시간 (ISO 8601 형식)
                String transactionKey,
                String receiptKey
        ) {}

}

// 💡 실제 취소 상세 내역을 바인딩할 서브 레코드

//        String cancellationKey,
//        String paymentKey,
//        String orderId,
//
//        String cancelReason,
//        String cancelledAt,
//        Long cancelAmount,
//        String status,
//
//
//        String code,
//        String message,
//        String error
//) {
//}
