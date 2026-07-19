package com.bubbletea.product.application.reservation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.bubbletea.product.application.reservation.executor.ReservationCommandExecutor;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductReservationExecutionServiceTest {

    @Mock
    private ProductChangeReservationRepository reservationRepository;

    @Mock
    private ProductChangeHistoryRepository historyRepository;

    @Mock
    private ReservationCommandExecutor activateExecutor;

    private ProductReservationExecutionService executionService;

    @BeforeEach
    void setUp() {
        when(activateExecutor.supports()).thenReturn(ReservationCommandType.ACTIVATE);

        executionService = new ProductReservationExecutionService(
            reservationRepository,
            historyRepository,
            List.of(activateExecutor),
            10L
        );
    }

    @Test
    @DisplayName("실행이 성공하면 markExecuted가 호출된다.")
    void executeReservation_success_callMarkExecuted() {
        // given
        ProductChangeReservation reservation =
            ProductChangeReservation.ofActivate("product1", LocalDateTime.now());

        given(reservationRepository.claimNextPending(any(), any()))
            .willReturn(Optional.of(reservation))
            .willReturn(Optional.empty());

        // when
        executionService.executeDueReservations(Set.of(ReservationCommandType.ACTIVATE));

        // then
        then(activateExecutor).should().execute(reservation);
        then(reservationRepository).should()
            .markExecuted(reservation.getId(), reservation.getClaimedAt());
        then(reservationRepository)
            .should(never()).markFailed(anyString(), anyString(), any());
        then(historyRepository).should(never()).save(any());
    }


}
