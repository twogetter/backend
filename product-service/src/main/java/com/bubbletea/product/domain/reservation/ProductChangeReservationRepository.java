package com.bubbletea.product.domain.reservation;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductChangeReservationRepository {

    boolean existsByProductIdAndReservationStatus(String productId, ReservationStatus status);

    void saveAll(List<ProductChangeReservation> reservations);

    Optional<ProductChangeReservation> claimNextPending(
        LocalDateTime now, Collection<ReservationCommandType> commandTypes);

    void markExecuted(String reservationId, LocalDateTime claimedAt);

    void markFailed(String reservationId, String failReason, LocalDateTime claimedAt);

    int recoverStalledProcessing(LocalDateTime staleBefore);

}
