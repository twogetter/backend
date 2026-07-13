package com.bubbletea.product.application.reservation;

import com.bubbletea.product.application.reservation.executor.ReservationCommandExecutor;
import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class ProductReservationExecutionService {

    private final ProductChangeReservationRepository reservationRepository;
    private final ProductChangeHistoryRepository productChangeHistoryRepository;
    private final Map<ReservationCommandType, ReservationCommandExecutor> executorsByType;
    private final long staleProcessingThresholdMinutes;

    public ProductReservationExecutionService(
        ProductChangeReservationRepository reservationRepository,
        ProductChangeHistoryRepository productChangeHistoryRepository,
        List<ReservationCommandExecutor> executors,
        @Value("${product.reservation.scheduler.stale-processing-threshold-minutes:10}")
        long staleProcessingThresholdMinutes
    ) {
        this.reservationRepository = reservationRepository;
        this.productChangeHistoryRepository = productChangeHistoryRepository;
        this.executorsByType = executors.stream()
            .collect(Collectors.toMap(ReservationCommandExecutor::supports, Function.identity()));
        this.staleProcessingThresholdMinutes = staleProcessingThresholdMinutes;
    }

    public void executeDueReservations(Collection<ReservationCommandType> targetCommandTypes) {

        LocalDateTime now = LocalDateTime.now();
        int executedCount = 0;

        Optional<ProductChangeReservation> claimed;

        while ((claimed = reservationRepository.claimNextPending(
            now, targetCommandTypes)).isPresent()
        ) {
            executeOne(claimed.get());
            executedCount++;
        }

        log.info("[예약 스케줄 실행] 대상 타입={}, 처리 건수={}", targetCommandTypes, executedCount);
    }

    public void recoverAndRetryStalledReservations() {
        LocalDateTime staleBefore = LocalDateTime.now()
            .minusMinutes(staleProcessingThresholdMinutes);
        int recovered = reservationRepository.recoverStalledProcessing(staleBefore);

        if (recovered == 0) {
            return;
        }

        executeDueReservations(Arrays.asList(ReservationCommandType.values()));

        log.warn("[예약 복구] PROCESSING 상태로 {}분 이상 멈춰있던 {}건을 PENDING으로 되돌리고 재시도",
            staleProcessingThresholdMinutes, recovered
        );
    }

    private void executeOne(ProductChangeReservation reservation) {
        ReservationCommandExecutor executor = executorsByType.get(reservation.getCommandType());

        if (executor == null) {
            log.error("[예약 실행 실패] 지원하지 않는 명령 reservationId={}, commandType={}",
                reservation.getId(), reservation.getCommandType());
            recordReservationFail(reservation, "지원하지 않는 명령: " + reservation.getCommandType());
            return;
        }

        try {
            executor.execute(reservation);
            reservationRepository.markExecuted(reservation.getId());

        } catch (Exception e) {
            log.error("[예약 실행 실패] reservationId={} ", reservation.getId(), e);
            String failReason =
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            recordReservationFail(reservation, failReason);
        }
    }

    private void recordReservationFail(ProductChangeReservation reservation, String failReason) {
        reservationRepository.markFailed(reservation.getId(), failReason);
        productChangeHistoryRepository.save(
            ProductChangeHistory.recordFailure(reservation, failReason));
    }

}
