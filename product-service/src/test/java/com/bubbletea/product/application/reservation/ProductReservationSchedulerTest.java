package com.bubbletea.product.application.reservation;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.then;

import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductReservationSchedulerTest {

    @InjectMocks
    private ProductReservationScheduler scheduler;

    @Mock
    private ProductReservationExecutionService executionService;

    @Test
    @DisplayName("NOTIFICATION 카테고리 타입들로 executeDueReservations를 호출한다.")
    void runNotifications_executesNotificationTypes() {
        // given
        Set<ReservationCommandType> expectedTypes = Arrays.stream(ReservationCommandType.values())
            .filter(type -> type.category() == ReservationCategory.NOTIFICATION)
            .collect(Collectors.toSet());

        // when
        scheduler.runNotifications();

        // then
        then(executionService).should().executeDueReservations(
            argThat(types ->
                types.size() == expectedTypes.size() &&
                    types.containsAll(expectedTypes)
            )
        );
    }

    @Test
    @DisplayName("STATUS_CHANGE 카테고리 타입들로 executeDueReservations를 호출한다.")
    void runStatusChanges_executesStatusChangeTypes() {
        // given
        Set<ReservationCommandType> expectedTypes = Arrays.stream(ReservationCommandType.values())
            .filter(type -> type.category() == ReservationCategory.STATUS_CHANGE)
            .collect(Collectors.toSet());

        // when
        scheduler.runStatusChanges();

        // then
        then(executionService).should().executeDueReservations(
            argThat(types ->
                types.size() == expectedTypes.size() &&
                    types.containsAll(expectedTypes)
            )
        );
    }

    @Test
    @DisplayName("PRICE_CHANGE 카테고리 타입들로 executeDueReservations를 호출한다.")
    void runPriceChanges_executesPriceChangeTypes() {
        // given
        Set<ReservationCommandType> expectedTypes = Arrays.stream(ReservationCommandType.values())
            .filter(type -> type.category() == ReservationCategory.PRICE_CHANGE)
            .collect(Collectors.toSet());

        // when
        scheduler.runPriceChanges();

        // then
        then(executionService).should().executeDueReservations(
            argThat(types ->
                types.size() == expectedTypes.size() &&
                    types.containsAll(expectedTypes)
            )
        );
    }

    @Test
    @DisplayName("recoverAndRetryStalledReservations를 호출한다.")
    void runStaleProcessingRecovery_callsRecovery() {
        // when
        scheduler.runStaleProcessingRecovery();

        // then
        then(executionService).should().recoverAndRetryStalledReservations();
    }
}