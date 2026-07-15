package com.bubbletea.product.infrastructure.mongo.reservation;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProductChangeReservationRepositoryImpl implements ProductChangeReservationRepository {

    private static final long EXECUTED_RETENTION_DAYS = 7L;
    private static final long FAILED_RETENTION_DAYS = 30L;

    private final ProductChangeReservationMongoRepository productChangeReservationMongoRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public boolean existsByProductIdAndReservationStatus(
        String productId, ReservationStatus status
    ) {
        return productChangeReservationMongoRepository
            .existsByProductIdAndStatus(productId, status);
    }

    @Override
    public void saveAll(List<ProductChangeReservation> reservations) {
        productChangeReservationMongoRepository.saveAll(reservations);
    }

    @Override
    public Optional<ProductChangeReservation> claimNextPending(
        LocalDateTime now,
        Collection<ReservationCommandType> commandTypes
    ) {
        Query query = Query.query(
                Criteria.where("status").is(ReservationStatus.PENDING)
                    .and("scheduledAt").lte(now)
                    .and("commandType").in(commandTypes))
            .with(Sort.by(Sort.Direction.ASC, "scheduledAt"))
            .limit(1);

        Update update = Update.update("status", ReservationStatus.PROCESSING)
            .set("claimedAt", LocalDateTime.now());

        ProductChangeReservation claimed = mongoTemplate.findAndModify(
            query, update, FindAndModifyOptions.options().returnNew(true),
            ProductChangeReservation.class
        );

        return Optional.ofNullable(claimed);
    }

    @Override
    public void markExecuted(String reservationId) {
        Update update = Update.update("status", ReservationStatus.EXECUTED)
            .set("executedAt", LocalDateTime.now())
            .set("expireAt", LocalDateTime.now().plusDays(EXECUTED_RETENTION_DAYS));

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("id").is(reservationId)), update,
            ProductChangeReservation.class);
    }

    @Override
    public void markFailed(String reservationId, String failReason) {
        Update update = Update.update("status", ReservationStatus.FAILED)
            .set("failReason", failReason)
            .set("expireAt", LocalDateTime.now().plusDays(FAILED_RETENTION_DAYS));

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("id").is(reservationId)), update,
            ProductChangeReservation.class);
    }

    @Override
    public int recoverStalledProcessing(LocalDateTime staleBefore) {
        Query query = Query.query(
            Criteria.where("status").is(ReservationStatus.PROCESSING)
                .and("claimedAt").lt(staleBefore));

        Update update = Update.update("status", ReservationStatus.PENDING).unset("claimedAt");

        return (int) mongoTemplate
            .updateMulti(query, update, ProductChangeReservation.class)
            .getModifiedCount();
    }
}
