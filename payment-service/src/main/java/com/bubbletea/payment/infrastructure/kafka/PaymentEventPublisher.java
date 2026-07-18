package com.bubbletea.payment.infrastructure.kafka;

import com.bubbletea.payment.infrastructure.kafka.dto.PaymentResultEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String SUCCESS_ORDER_TOPIC = "order.payment.paymentSuccess";
    private static final String SUCCESS_NOTIFICATION_TOPIC = "notification.payment.paymentComplete";
    private static final String FAILED_ORDER_TOPIC = "order.payment.paymentFailed";
    private static final String FAILED_NOTIFICATION_TOPIC = "notification.payment.paymentFail";
    private static final String HOLD_ORDER_TOPIC = "order.payment.paymentHold";
    private static final String HOLD_NOTIFICATION_TOPIC = "notification.payment.paymentHold";

    private static final String CANCEL_SUCCESS_ORDER_TOPIC = "order.payment.paymentCancelSuccess";
    private static final String CANCEL_SUCCESS_NOTIFICATION_TOPIC = "notification.payment.paymentCancelComplete";
    private static final String CANCEL_FAILED_ORDER_TOPIC = "order.payment.paymentCancelFailed";
    private static final String CANCEL_FAILED_NOTIFICATION_TOPIC = "notification.payment.paymentCancelFail";
    private static final String CANCEL_HOLD_ORDER_TOPIC = "order.payment.paymentCancelHold";
    private static final String CANCEL_HOLD_NOTIFICATION_TOPIC = "notification.payment.paymentCancelHold";


    private static final String HEADER_DOMAIN = "X-Domain";
    private static final String HEADER_EVENT_TYPE = "X-Event-Type";
    private static final String HEADER_EVENT_TIMESTAMP = "X-Event-Timestamp";

    public void publishPaymentSuccess(PaymentResultEvent event) {
        publish(event, SUCCESS_ORDER_TOPIC, "paymentComplete");
        publish(event, SUCCESS_NOTIFICATION_TOPIC, "paymentComplete");
    }

    public void publishPaymentFailed(PaymentResultEvent event) {
        publish(event, FAILED_ORDER_TOPIC, "paymentFail");
        publish(event, FAILED_NOTIFICATION_TOPIC, "paymentFail");
    }

    public void publishPaymentHold(PaymentResultEvent event) {
        publish(event, HOLD_ORDER_TOPIC, "paymentHold");
        publish(event, HOLD_NOTIFICATION_TOPIC, "paymentHold");
    }

    public void publishCancelPaymentSuccess(PaymentResultEvent event) {
        publish(event, CANCEL_SUCCESS_ORDER_TOPIC, "paymentCancelComplete");
        publish(event, CANCEL_SUCCESS_NOTIFICATION_TOPIC, "paymentCancelComplete");
    }

    public void publishCancelPaymentFailed(PaymentResultEvent event) {
        publish(event, CANCEL_FAILED_ORDER_TOPIC, "paymentCancelFail");
        publish(event, CANCEL_FAILED_NOTIFICATION_TOPIC, "paymentCancelFail");
    }

    public void publishCancelPaymentHold(PaymentResultEvent event) {
        publish(event, CANCEL_HOLD_ORDER_TOPIC, "paymentCancelHold");
        publish(event, CANCEL_HOLD_NOTIFICATION_TOPIC, "paymentCancelHold");
    }

    private void publish(PaymentResultEvent event, String topic, String eventType) {
        String messageKey = event.orderId().toString();

        String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Message<PaymentResultEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, messageKey)
                .setHeader(HEADER_DOMAIN, "payment")
                .setHeader(HEADER_EVENT_TYPE, eventType)
                .setHeader(HEADER_EVENT_TIMESTAMP, currentTimestamp)
                .build();

        log.info("[Kafka Producer] 이벤트 발행 시도 -> Topic: {}, EventType: {}, OrderId: {}", topic, eventType, event.orderId());

        kafkaTemplate.send(message).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[Kafka Producer] 이벤트 발행 성공. Topic: {}, Partition: {}, Offset: {}", topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[Kafka Producer] 이벤트 발행 실패. Topic: {}, Error: {}", topic, ex.getMessage(), ex);
            }
        });
    }
}
