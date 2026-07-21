package com.bubbletea.product.application.reservation;


import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.reservation.dto.ReservationDetailResponseDto;
import com.bubbletea.product.application.reservation.dto.ReservationListResponseDto;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationSearchCondition;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReservationQueryService {

    private final ProductChangeReservationRepository reservationRepository;

    public ReservationDetailResponseDto getReservation(String reservationId) {
        ProductChangeReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new AppException(ProductErrorCode.RESERVATION_NOT_FOUND));
        return ReservationDetailResponseDto.from(reservation);
    }

    public ReservationListResponseDto getReservations(
        ReservationCategory category, ReservationStatus status, Pageable pageable
    ) {
        ReservationSearchCondition condition = new ReservationSearchCondition(category, status);
        Page<ProductChangeReservation> page = reservationRepository.findAll(condition, pageable);
        return ReservationListResponseDto.from(page);
    }

}
