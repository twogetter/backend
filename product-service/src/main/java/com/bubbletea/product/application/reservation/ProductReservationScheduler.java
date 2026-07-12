package com.bubbletea.product.application.reservation;

import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProductReservationScheduler {

    private final ProductReservationExecutionService productReservationExecutionService;

    @Scheduled(cron = "${product.reservation.scheduler.notification-cron:0 0 15 * * *}")
    public void runNotifications() {
        run(ReservationCategory.NOTIFICATION);
    }

    @Scheduled(cron = "${product.reservation.scheduler.status-change-cron:0 0 15 * * *}")
    public void runStatusChanges() {
        run(ReservationCategory.STATUS_CHANGE);
    }

    @Scheduled(cron = "${product.reservation.scheduler.price-change-cron:0 0 0 * * *}")
    public void runPriceChanges() {
        run(ReservationCategory.PRICE_CHANGE);
    }

    @Scheduled(fixedDelayString = "${product.reservation.scheduler.recovery-fixed-delay-ms:300000}")
    public void runStaleProcessingRecovery() {
        productReservationExecutionService.recoverAndRetryStalledReservations();
    }

    private void run(ReservationCategory category) {
        Set<ReservationCommandType> targetTypes = Arrays.stream(ReservationCommandType.values())
            .filter(type -> type.category() == category)
            .collect(Collectors.toSet());

        log.info("[예약 스케줄러] {} 카테고리 실행 시작", category);
        productReservationExecutionService.executeDueReservations(targetTypes);
        log.info("[예약 스케줄러] {} 카테고리 실행 종료", category);
    }

}
