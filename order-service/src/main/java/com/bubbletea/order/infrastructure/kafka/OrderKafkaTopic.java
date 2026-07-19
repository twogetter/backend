package com.bubbletea.order.infrastructure.kafka;

public class OrderKafkaTopic {

  public static final String RENEWAL_SUBSCRIPTION = "notifiacation.order.subscriptionRenewal";

  public static final String SUBSCRIPTION_ACTIVATED = "chat.order.subscriptionActivated";

  public static final String PAYMENT_FAILED = "notification.order.paymentFailed";

  private OrderKafkaTopic() {}
}
