package com.bubbletea.product.application.reservation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.bubbletea.product.application.reservation.executor.ReservationCommandExecutor;
import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationFixture;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Mock
    private ReservationCommandExecutor deletionExecutor;

    private ProductReservationExecutionService executionService;

    @BeforeEach
    void setUp() {
        given(activateExecutor.supports()).willReturn(ReservationCommandType.ACTIVATE);
        given(deletionExecutor.supports()).willReturn(ReservationCommandType.DELETION);

        executionService = new ProductReservationExecutionService(
            reservationRepository,
            historyRepository,
            List.of(activateExecutor, deletionExecutor),
            10L
        );
    }

    private ProductChangeReservation createMockedReservation(
        String id, ReservationCommandType commandType, LocalDateTime claimedAt
    ) {
        ProductChangeReservation reservation = mock(ProductChangeReservation.class);
        given(reservation.getId()).willReturn(id);
        given(reservation.getCommandType()).willReturn(commandType);
        given(reservation.getClaimedAt()).willReturn(claimedAt);
        return reservation;
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

    @Nested
    @DisplayName("executeDueReservations()")
    class ExecuteDueReservations {

        @Test
        @DisplayName("대기 중인 예약이 없으면 executor를 실행하지 않는다.")
        void noPendingReservations_doesNotExecute() {
            // given
            given(reservationRepository.claimNextPending(any(LocalDateTime.class), anyCollection()))
                .willReturn(Optional.empty());

            // when
            executionService.executeDueReservations(List.of(ReservationCommandType.ACTIVATE));

            // then
            then(activateExecutor).should(never()).execute(any());
            then(reservationRepository).should(never()).markExecuted(any(), any());
        }

        @Test
        @DisplayName("대기 중인 예약이 있으면 해당 executor를 실행하고 완료 처리한다.")
        void pendingReservationExists_executesAndMarksExecuted() {
            // given
            ProductChangeReservation reservation = ProductChangeReservationFixture.activate();

            given(reservationRepository.claimNextPending(any(LocalDateTime.class), anyCollection()))
                .willReturn(Optional.of(reservation))
                .willReturn(Optional.empty());

            // when
            executionService.executeDueReservations(List.of(ReservationCommandType.ACTIVATE));

            // then
            then(activateExecutor).should().execute(reservation);
            then(reservationRepository).should()
                .markExecuted(reservation.getId(), reservation.getClaimedAt());
            then(historyRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("대기 중인 예약이 여러 개 있으면 모두 실행한다.")
        void multiplePendingReservations_executesAll() {
            // given
            ProductChangeReservation reservation1 = ProductChangeReservationFixture.activate(
                "product-1", LocalDateTime.of(2026, 7, 1, 0, 0)
            );
            ProductChangeReservation reservation2 = ProductChangeReservationFixture.activate(
                "product-2", LocalDateTime.of(2026, 7, 2, 0, 0)
            );

            given(reservationRepository.claimNextPending(any(LocalDateTime.class), anyCollection()))
                .willReturn(Optional.of(reservation1))
                .willReturn(Optional.of(reservation2))
                .willReturn(Optional.empty());

            // when
            executionService.executeDueReservations(List.of(ReservationCommandType.ACTIVATE));

            // then
            then(activateExecutor).should(times(2))
                .execute(any(ProductChangeReservation.class));
            then(reservationRepository).should(times(2))
                .markExecuted(any(), any());
        }

        @Test
        @DisplayName("지원하지 않는 commandType이면 실패 이력을 저장한다.")
        void unsupportedCommandType_recordsFailure() {
            // given
            String reservationId = "reservation-unsupported";
            LocalDateTime claimedAt = LocalDateTime.of(2026, 7, 23, 0, 0);

            ProductChangeReservation reservation = createMockedReservation(
                reservationId, ReservationCommandType.NOTIFY_DELETION_SCHEDULE, claimedAt
            );

            given(reservationRepository.claimNextPending(any(LocalDateTime.class), anyCollection()))
                .willReturn(Optional.of(reservation))
                .willReturn(Optional.empty());

            given(reservationRepository.markFailed(
                eq(reservationId), anyString(), eq(claimedAt))
            ).willReturn(true);

            given(historyRepository.save(any(ProductChangeHistory.class)))
                .willReturn(mock(ProductChangeHistory.class));

            // when
            executionService.executeDueReservations(
                List.of(ReservationCommandType.NOTIFY_DELETION_SCHEDULE));

            // then
            then(reservationRepository).should()
                .markFailed(eq(reservationId), anyString(), eq(claimedAt));
            then(historyRepository).should().save(any(ProductChangeHistory.class));
            then(reservationRepository).should(never()).markExecuted(any(), any());
        }

        @Test
        @DisplayName("executor 실행 중 예외가 발생하면 실패 이력을 저장한다.")
        void executorThrowsException_recordsFailure() {
            // given
            String reservationId = "reservation-error";
            LocalDateTime claimedAt = LocalDateTime.of(2026, 7, 23, 0, 0);
            String errorMessage = "DB 연결 오류";

            ProductChangeReservation reservation = createMockedReservation(
                reservationId, ReservationCommandType.ACTIVATE, claimedAt
            );

            given(reservationRepository.claimNextPending(any(LocalDateTime.class), anyCollection()))
                .willReturn(Optional.of(reservation))
                .willReturn(Optional.empty());

            willAnswer(invocation -> {
                throw new RuntimeException(errorMessage);
            }).given(activateExecutor).execute(reservation);

            given(reservationRepository.markFailed(
                eq(reservationId), eq(errorMessage), eq(claimedAt))
            ).willReturn(true);

            given(historyRepository.save(any(ProductChangeHistory.class)))
                .willReturn(mock(ProductChangeHistory.class));

            // when
            executionService.executeDueReservations(List.of(ReservationCommandType.ACTIVATE));

            // then
            then(reservationRepository).should(never()).markExecuted(any(), any());
            then(reservationRepository).should()
                .markFailed(eq(reservationId), eq(errorMessage), eq(claimedAt));
            then(historyRepository).should().save(any(ProductChangeHistory.class));
        }

        @Test
        @DisplayName("executor 실행 중 예외 발생 후 markFailed가 false를 반환하면 이력을 저장하지 않는다.")
        void executorThrowsException_markFailedReturnsFalse_doesNotSaveHistory() {
            // given
            ProductChangeReservation reservation = ProductChangeReservationFixture.activate();

            given(reservationRepository.claimNextPending(any(LocalDateTime.class), anyCollection()))
                .willReturn(Optional.of(reservation))
                .willReturn(Optional.empty());

            willAnswer(invocation -> {
                throw new RuntimeException("오류");
            }).given(activateExecutor).execute(reservation);

            given(reservationRepository.markFailed(
                eq(reservation.getId()), anyString(), eq(reservation.getClaimedAt()))
            ).willReturn(false);

            // when
            executionService.executeDueReservations(List.of(ReservationCommandType.ACTIVATE));

            // then
            then(historyRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("executor 실행 중 메시지 없는 예외가 발생하면 클래스 이름을 실패 사유로 저장한다.")
        void executorThrowsExceptionWithoutMessage_usesClassNameAsFailReason() {
            // given
            String reservationId = "reservation-no-msg";
            LocalDateTime claimedAt = LocalDateTime.of(2026, 7, 23, 0, 0);
            String expectedFailReason = "NullPointerException";

            ProductChangeReservation reservation = createMockedReservation(
                reservationId, ReservationCommandType.ACTIVATE, claimedAt
            );

            given(reservationRepository.claimNextPending(any(LocalDateTime.class), anyCollection()))
                .willReturn(Optional.of(reservation))
                .willReturn(Optional.empty());

            willAnswer(invocation -> {
                throw new NullPointerException(); // message == null
            }).given(activateExecutor).execute(reservation);

            given(reservationRepository.markFailed(
                eq(reservationId), eq(expectedFailReason), eq(claimedAt))
            ).willReturn(true);

            given(historyRepository.save(any(ProductChangeHistory.class)))
                .willReturn(mock(ProductChangeHistory.class));

            // when
            executionService.executeDueReservations(List.of(ReservationCommandType.ACTIVATE));

            // then
            then(reservationRepository).should()
                .markFailed(eq(reservationId), eq(expectedFailReason), eq(claimedAt));
        }
    }

    @Nested
    @DisplayName("recoverAndRetryStalledReservations()")
    class RecoverAndRetryStalledReservations {

        @Test
        @DisplayName("stalled 예약이 없으면 executeDueReservations를 호출하지 않는다")
        void noStalledReservations_doesNotExecute() {
            // given
            given(reservationRepository.recoverStalledProcessing(any(LocalDateTime.class)))
                .willReturn(0);

            // when
            executionService.recoverAndRetryStalledReservations();

            // then
            then(reservationRepository).should(never())
                .claimNextPending(any(), anyCollection());
        }

        @Test
        @DisplayName("stalled 예약이 있으면 모든 타입에 대해 executeDueReservations를 실행한다")
        void stalledReservationsExist_executesAllCommandTypes() {
            // given
            given(reservationRepository.recoverStalledProcessing(any(LocalDateTime.class)))
                .willReturn(2);

            given(reservationRepository.claimNextPending(any(LocalDateTime.class), anyCollection()))
                .willReturn(Optional.empty());

            // when
            executionService.recoverAndRetryStalledReservations();

            // then
            then(reservationRepository).should()
                .claimNextPending(any(LocalDateTime.class), anyCollection());
        }

    }


}
