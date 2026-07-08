package com.bubbletea.product.presentation.controller;

import com.bubbletea.product.application.service.ProductChatEventService;
import com.bubbletea.product.application.service.ProductScheduleNotificationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kafka 이벤트 발행을 위한 임시 mock API.
 */
@RestController
@RequestMapping("/api/products/mock-events")
@RequiredArgsConstructor
public class MockProductEventController {

    private final ProductChatEventService productChatEventService;
    private final ProductScheduleNotificationService productScheduleNotificationService;

    private static final String productName = "김연옌 1개월 구독권";
    private static final LocalDateTime targetDate = LocalDateTime.now().plusDays(3);

    @PostMapping("/register")
    public ResponseEntity<Void> mockEvent() {
        String artistId = "1";
        productChatEventService.productRegistered(artistId, targetDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/open")
    public ResponseEntity<Void> mockOpenEvent() {
        productScheduleNotificationService.notifyOpenSchedule(productName, targetDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> mockDeactivateEvent() {
        productScheduleNotificationService.notifyDeactivationSchedule(productName, targetDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> mockActivateEvent() {
        productScheduleNotificationService.notifyActivationSchedule(productName, targetDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> mockDeleteEvent() {
        productScheduleNotificationService.notifyDeletionSchedule(productName, targetDate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/price-change")
    public ResponseEntity<Void> mockPriceChangeEvent() {
        int originPrice = 5900;
        int changePrice = 6900;
        productScheduleNotificationService.notifyPriceChangeSchedule(
            productName, targetDate, originPrice, changePrice
        );
        return ResponseEntity.ok().build();
    }

}
