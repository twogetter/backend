package com.bubbletea.product.application.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.reservation.dto.ReservationDetailResponseDto;
import com.bubbletea.product.application.reservation.dto.ReservationListResponseDto;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationFixture;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationSearchCondition;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductReservationQueryServiceTest {

    @InjectMocks
    private ProductReservationQueryService queryService;

    @Mock
    private ProductChangeReservationRepository reservationRepository;

    @Nested
    @DisplayName("getReservation()")
    class GetReservation {

        @Test
        @DisplayName("존재하는 reservationId로 조회하면 상세 응답 DTO를 반환한다.")
        void existingReservationId_returnsDetailDto() {
            // given
            String reservationId = "reservation-1";

            ProductChangeReservation reservation = ProductChangeReservationFixture.deletion();
            given(reservationRepository.findById(reservationId))
                .willReturn(Optional.of(reservation));

            // when
            ReservationDetailResponseDto result = queryService.getReservation(reservationId);

            // then
            assertThat(result).isNotNull();
            then(reservationRepository).should().findById(reservationId);
        }

        @Test
        @DisplayName("존재하지 않는 reservationId로 조회하면 AppException을 던진다.")
        void nonExistingReservationId_throwsAppException() {
            // given
            String reservationId = "not-existing";
            given(reservationRepository.findById(reservationId))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queryService.getReservation(reservationId))
                .isInstanceOf(AppException.class);

            then(reservationRepository).should().findById(reservationId);
        }
    }

    @Nested
    @DisplayName("getReservations()")
    class GetReservations {

        @Test
        @DisplayName("카테고리와 상태로 예약 목록을 페이징 조회한다.")
        void validCondition_returnsPagedList() {
            // given
            ReservationCategory category = ReservationCategory.NOTIFICATION;
            ReservationStatus status = ReservationStatus.PENDING;
            Pageable pageable = PageRequest.of(0, 10);

            ProductChangeReservation reservation = ProductChangeReservationFixture.deletion();
            Page<ProductChangeReservation> page = new PageImpl<>(
                List.of(reservation), pageable, 1
            );

            given(reservationRepository.findAll(
                any(ReservationSearchCondition.class), eq(pageable))
            ).willReturn(page);

            // when
            ReservationListResponseDto result = queryService.getReservations(
                category, status, pageable);

            // then
            assertThat(result).isNotNull();
            then(reservationRepository).should()
                .findAll(any(ReservationSearchCondition.class), eq(pageable));
        }

        @Test
        @DisplayName("조회 결과가 비어있으면 빈 목록 DTO를 반환한다.")
        void emptyResult_returnsEmptyListDto() {
            // given
            ReservationCategory category = ReservationCategory.STATUS_CHANGE;
            ReservationStatus status = ReservationStatus.EXECUTED;
            Pageable pageable = PageRequest.of(0, 10);

            Page<ProductChangeReservation> emptyPage = new PageImpl<>(
                List.of(), pageable, 0
            );

            given(reservationRepository.findAll(
                any(ReservationSearchCondition.class), eq(pageable))
            ).willReturn(emptyPage);

            // when
            ReservationListResponseDto result = queryService.getReservations(
                category, status, pageable);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("category와 status가 null이어도 정상적으로 조회한다.")
        void nullCategoryAndStatus_returnsResult() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProductChangeReservation> emptyPage = new PageImpl<>(
                List.of(), pageable, 0
            );

            given(reservationRepository.findAll(
                any(ReservationSearchCondition.class), eq(pageable))
            ).willReturn(emptyPage);

            // when
            ReservationListResponseDto result = queryService.getReservations(
                null, null, pageable);

            // then
            assertThat(result).isNotNull();
            then(reservationRepository).should()
                .findAll(any(ReservationSearchCondition.class), eq(pageable));
        }
    }
}