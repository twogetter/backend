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

    @PostMapping("/register")
    public ResponseEntity<Void> mockEvent() {
        String artistId = "1";
        productChatEventService.productRegistered(artistId, LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/open")
    public ResponseEntity<Void> mockOpenEvent() {
        productScheduleNotificationService.notifyOpenSchedule(productName,
            LocalDateTime.now().plusDays(3)
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> mockDeactivateEvent() {
        productScheduleNotificationService.notifyDeactivationSchedule(
            productName, LocalDateTime.now().plusDays(3)
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> mockActivateEvent() {
        productScheduleNotificationService.notifyActivationSchedule(
            productName, LocalDateTime.now().plusDays(3)
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> mockDeleteEvent() {
        productScheduleNotificationService.notifyDeletionSchedule(
            productName, LocalDateTime.now().plusDays(3)
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/price-change")
    public ResponseEntity<Void> mockPriceChangeEvent() {
        int originPrice = 5900;
        int changePrice = 6900;
        productScheduleNotificationService.notifyPriceChangeSchedule(
            productName, LocalDateTime.now().plusDays(3), originPrice, changePrice
        );
        return ResponseEntity.ok().build();
    }

}
