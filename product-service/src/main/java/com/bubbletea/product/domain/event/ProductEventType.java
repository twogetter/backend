package com.bubbletea.product.domain.event;


public enum ProductEventType {

    PRODUCT_REGISTERED("chat", "productRegistered"),
    PRODUCT_OPEN_SCHEDULE("notification", "productOpenScheduled"),
    PRODUCT_ACTIVATION_SCHEDULE("notification", "productActivationScheduled"),
    PRODUCT_DEACTIVATION_SCHEDULE("notification", "productDeactivationScheduled"),
    PRODUCT_DELETION_SCHEDULE("notification", "productDeletionScheduled"),
    PRODUCT_PRICE_CHANGE_SCHEDULE("notification", "productPriceChangeScheduled");

    private static final String PRODUCER_DOMAIN = "product";

    private final String consumerDomain;
    private final String eventName;

    ProductEventType(String consumerDomain, String eventName) {
        this.consumerDomain = consumerDomain;
        this.eventName = eventName;
    }

    public String topic() {
        return consumerDomain + "." + PRODUCER_DOMAIN + "." + eventName;
    }

}
