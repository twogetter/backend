package com.bubbletea.order.domain.exception;

import com.bubbletea.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

  PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "ORDER_001", "결제 처리에 실패했습니다."),
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_002", "주문을 찾을 수 없습니다."),
  OUTBOX_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_003", "이벤트 직렬화에 실패했습니다."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "ORDER_004", "잘못된 요청입니다."),
  SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_005", "구독을 찾을 수 없습니다."),
  // PENDING(결제 처리 중)도 점유로 보므로 두 경우를 한 문구로 묶는다.
  DUPLICATE_SUBSCRIPTION(HttpStatus.CONFLICT, "ORDER_006", "이미 구독 중이거나 처리 중인 상품입니다."),
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
