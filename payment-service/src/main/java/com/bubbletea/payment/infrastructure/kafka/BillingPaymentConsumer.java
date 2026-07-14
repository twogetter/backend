package com.bubbletea.payment.infrastructure.kafka;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.payment.facade.BillingPaymentFacade;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.global.exception.PaymentTossApiException;
import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPaymentConsumer {

    private final BillingPaymentFacade billingPaymentFacade;

    @KafkaListener(
            topics = "payment.order.payment-requested",
            groupId = "billing-payment-group",
            concurrency = "3"
    )
    public void consumeBillingEvent(
            ConsumerRecord<String, BillingEvent> record,
            Acknowledgment ack,
            @Header(name = "X-User-Id") String userIdStr
    ) {

        Long orderId = Long.parseLong(record.key());
        Long userId = Long.parseLong(userIdStr);

        try {
            BillingEvent event = record.value();
            billingPaymentFacade.executeBilling(userId, orderId, event);
            ack.acknowledge();
        } catch (AppException e) {

            if (e.getErrorCode() == PaymentErrorCode.EXTERNAL_SERVER_ERROR) {
                log.warn("외부 서버 타임아웃 발생 (상태 미확인) - 카프카 자체 재시도를 위해 예외 전달 (OrderId: {})", orderId);
                throw e;
            }
            // 잔액 부족, 한도 초과 등 재시도해도 안 되는 명확한 비즈니스 실패인 경우
            log.error("결제 비즈니스 로직 실패 (ErrorCode: {}, OrderId: {})", e.getErrorCode(), orderId);
            ack.acknowledge();
        }
    }
}

