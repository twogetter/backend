package com.bubbletea.order.infrastructure.kafka;

public class OrderKafkaTopic {

  public static final String RENEWAL_SUBSCRIPTION = "notification.order.subscriptionRenewal";

  public static final String SUBSCRIPTION_ACTIVATED = "chat.order.subscriptionActivated";

  public static final String PAYMENT_FAILED = "notification.order.paymentFailed";

  /** 정기결제 배치가 결제를 요청할 때 발행하는 토픽(payment-service가 소비). */
  public static final String PAYMENT_REQUESTED = "payment.order.payment-requested";

  // payment-service가 발행하는 결제 결과 토픽(order-service가 소비).
  public static final String PAYMENT_RESULT_SUCCESS = "order.payment.paymentSuccess";
  public static final String PAYMENT_RESULT_FAILED = "order.payment.paymentFailed";
  public static final String PAYMENT_RESULT_HOLD = "order.payment.paymentHold";

  private OrderKafkaTopic() {}
}
