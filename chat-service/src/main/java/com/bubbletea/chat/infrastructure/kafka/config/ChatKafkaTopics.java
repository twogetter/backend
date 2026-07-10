package com.bubbletea.chat.infrastructure.kafka.config;

public final class ChatKafkaTopics {

  // 수신
  public static final String PRODUCT_REGISTERED = "chat.product.productRegistered";
  public static final String ORDER_CREATED = "chat.order.created";
  public static final String ORDER_EXPIRED = "chat.order.expired";
  public static final String MEMBER_WITHDRAWN = "chat.member.withdrawn";

  // 송신
  public static final String CHAT_PUBLISHED = "notification.chat.published";

  private ChatKafkaTopics() {
  }
}
