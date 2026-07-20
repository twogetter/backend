package com.bubbletea.payment.entity.enums;

import java.util.List;
import lombok.Getter;

@Getter
public enum PaymentEventType {

    SUCCESS(
            "paymentSuccess",
            List.of("order.payment.paymentSuccess", "notification.payment.paymentComplete")
    ),
    FAILED(
            "paymentFailed",
            List.of("order.payment.paymentFailed", "notification.payment.paymentFail")
    ),
    HOLD(
            "paymentHold",
            List.of("order.payment.paymentHold", "notification.payment.paymentHold")
    ),
    CANCEL_SUCCESS(
            "paymentCancelSuccess",
            List.of("order.payment.paymentCancelSuccess", "notification.payment.paymentCancelComplete")
    ),
    CANCEL_FAILED(
            "paymentCancelFailed",
            List.of("order.payment.paymentCancelFailed", "notification.payment.paymentCancelFail")
    ),
    CANCEL_HOLD(
            "paymentCancelHold",
            List.of("order.payment.paymentCancelHold", "notification.payment.paymentCancelHold")
    );

    private final String eventTypeHeader; // 카프카 헤더에 들어갈 값 (예: "paymentCancelHold")
    private final List<String> topics;    // 발행해야 하는 카프카 토픽 리스트

    PaymentEventType(String eventTypeHeader, List<String> topics) {
        this.eventTypeHeader = eventTypeHeader;
        this.topics = topics;
    }
}
