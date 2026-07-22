package com.bubbletea.order.infrastructure.kafka.consumer;

import com.bubbletea.order.application.BillingResultService;
import com.bubbletea.order.domain.event.PaymentResultEvent;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * payment-service가 발행하는 결제 결과(성공/실패/보류)를 소비해 정기결제 회차를 확정한다.
 * 세 토픽 모두 {@code PaymentResultEvent} 페이로드이며, 처리 분기는 {@code status} 값으로 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultConsumer {

  private final BillingResultService billingResultService;

  @KafkaListener(
      topics = {
          OrderKafkaTopic.PAYMENT_RESULT_SUCCESS,
          OrderKafkaTopic.PAYMENT_RESULT_FAILED,
          OrderKafkaTopic.PAYMENT_RESULT_HOLD
      },
      containerFactory = "paymentResultListenerContainerFactory"
  )
  public void consume(PaymentResultEvent event) {
    log.info("[Kafka Consumer] 결제 결과 수신. orderId={}, status={}", event.orderId(), event.status());
    billingResultService.handle(event);
  }
}
