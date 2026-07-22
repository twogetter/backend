package com.bubbletea.product.domain.outbox;

import java.util.Map;


public interface OutboxMessageSender {

    void send(String topic, String payload, Map<String, String> headers);
}
