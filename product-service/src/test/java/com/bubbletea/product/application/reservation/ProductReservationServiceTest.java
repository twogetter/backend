package com.bubbletea.product.application.reservation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import com.bubbletea.product.presentation.dto.ProductDeletionReservationRequestDto;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductReservationServiceTest {

    @InjectMocks
    private ProductReservationService reservationService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductChangeReservationRepository productChangeReservationRepository;

    @Nested
    @DisplayName("reserveDeletion()")
    class ReserveDeletion {

        @Test
        @DisplayName("정상 요청 시 알림 예약과 삭제 예약 2건을 저장한다.")
        void validRequest_savesTwoReservations() {
            // given
            String productId = "product-1";
            LocalDateTime deleteDate = LocalDateTime.of(2026, 7, 23, 12, 0);
            LocalDateTime expectedNotifyAt = deleteDate.minusDays(3);
            String productName = "[김연옌] 구독권";

            Product product = mock(Product.class);
            given(product.getName()).willReturn(productName);

            ProductDeletionReservationRequestDto request =
                mock(ProductDeletionReservationRequestDto.class);
            given(request.deleteDate()).willReturn(deleteDate);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(productChangeReservationRepository.existsByProductIdAndReservationStatus(
                productId, ReservationStatus.PENDING)
            ).willReturn(false);

            // when
            reservationService.reserveDeletion(productId, request);

            // then
            then(product).should().verifySchedulability(deleteDate);
            then(productChangeReservationRepository).should().saveAll(
                argThat(reservations -> {

                    boolean hasNotify = reservations.stream().anyMatch(r ->
                        r.getCommandType() == ReservationCommandType.NOTIFY_DELETION_SCHEDULE
                    );
                    boolean hasDeletion = reservations.stream().anyMatch(r ->
                        r.getCommandType() == ReservationCommandType.DELETION
                    );
                    boolean notifyScheduledCorrectly = reservations.stream()
                        .filter(r ->
                            r.getCommandType() == ReservationCommandType.NOTIFY_DELETION_SCHEDULE)
                        .allMatch(r -> r.getScheduledAt().equals(expectedNotifyAt));
                    boolean deletionScheduledCorrectly = reservations.stream()
                        .filter(r -> r.getCommandType() == ReservationCommandType.DELETION)
                        .allMatch(r -> r.getScheduledAt().equals(deleteDate));

                    return reservations.size() == 2
                        && hasNotify
                        && hasDeletion
                        && notifyScheduledCorrectly
                        && deletionScheduledCorrectly;
                })
            );
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 AppException을 던진다.")
        void productNotFound_throwsAppException() {
            // given
            String productId = "not-existing";
            ProductDeletionReservationRequestDto request =
                mock(ProductDeletionReservationRequestDto.class);

            given(productRepository.findById(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.reserveDeletion(productId, request))
                .isInstanceOf(AppException.class);

            then(productChangeReservationRepository).should(never())
                .existsByProductIdAndReservationStatus(any(), any());
            then(productChangeReservationRepository).should(never()).saveAll(anyList());
        }

        @Test
        @DisplayName("이미 PENDING 예약이 존재하면 AppException을 던진다.")
        void alreadyPendingReservation_throwsAppException() {
            // given
            String productId = "product-1";

            Product product = mock(Product.class);
            ProductDeletionReservationRequestDto request =
                mock(ProductDeletionReservationRequestDto.class);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(productChangeReservationRepository.existsByProductIdAndReservationStatus(
                productId, ReservationStatus.PENDING)
            ).willReturn(true);

            // when & then
            assertThatThrownBy(() -> reservationService.reserveDeletion(productId, request))
                .isInstanceOf(AppException.class);

            then(productChangeReservationRepository).should(never()).saveAll(anyList());
        }

        @Test
        @DisplayName("verifySchedulability 검증 실패 시 AppException을 던진다.")
        void verifySchedulabilityFails_throwsAppException() {
            // given
            String productId = "product-1";
            LocalDateTime invalidDeleteDate = LocalDateTime.now().minusDays(1);

            Product product = mock(Product.class);
            ProductDeletionReservationRequestDto request =
                mock(ProductDeletionReservationRequestDto.class);
            given(request.deleteDate()).willReturn(invalidDeleteDate);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(productChangeReservationRepository.existsByProductIdAndReservationStatus(
                productId, ReservationStatus.PENDING)
            ).willReturn(false);

            willThrow(AppException.class)
                .given(product).verifySchedulability(any(LocalDateTime.class));

            // when & then
            assertThatThrownBy(() -> reservationService.reserveDeletion(productId, request))
                .isInstanceOf(AppException.class);

            then(productChangeReservationRepository).should(never()).saveAll(anyList());
        }

        @Test
        @DisplayName("알림 예약의 scheduledAt은 deleteDate 3일 전으로 설정된다.")
        void notifyReservation_scheduledAtIsThreeDaysBeforeDeleteDate() {
            // given
            String productId = "product-1";
            LocalDateTime deleteDate = LocalDateTime.of(2026, 7, 23, 12, 0);
            LocalDateTime expectedNotifyAt = deleteDate.minusDays(3);

            Product product = mock(Product.class);
            given(product.getName()).willReturn("[김연옌] 구독권");

            ProductDeletionReservationRequestDto request =
                mock(ProductDeletionReservationRequestDto.class);
            given(request.deleteDate()).willReturn(deleteDate);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(productChangeReservationRepository.existsByProductIdAndReservationStatus(
                productId, ReservationStatus.PENDING)
            ).willReturn(false);

            // when
            reservationService.reserveDeletion(productId, request);

            // then
            then(productChangeReservationRepository).should().saveAll(
                argThat(reservations -> reservations.stream()
                    .filter(r ->
                        r.getCommandType() == ReservationCommandType.NOTIFY_DELETION_SCHEDULE)
                    .anyMatch(r -> r.getScheduledAt().equals(expectedNotifyAt)))
            );
        }
    }
}