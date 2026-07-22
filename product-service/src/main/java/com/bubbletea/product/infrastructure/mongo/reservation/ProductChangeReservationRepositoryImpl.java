package com.bubbletea.product.infrastructure.mongo.reservation;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import com.bubbletea.product.domain.reservation.ReservationSearchCondition;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import com.mongodb.client.result.UpdateResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
class ProductChangeReservationRepositoryImpl implements ProductChangeReservationRepository {

    private static final long EXECUTED_RETENTION_DAYS = 7L;
    private static final long FAILED_RETENTION_DAYS = 30L;

    private final ProductChangeReservationMongoRepository productChangeReservationMongoRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<ProductChangeReservation> findById(String reservationId) {
        return productChangeReservationMongoRepository.findById(reservationId);
    }

    @Override
    public Page<ProductChangeReservation> findAll(
        ReservationSearchCondition condition,
        Pageable pageable
    ) {
        Criteria criteria = buildCriteria(condition);

        Query query = Query.query(criteria).with(pageable);
        List<ProductChangeReservation> content = mongoTemplate.find(query,
            ProductChangeReservation.class);

        long total = mongoTemplate.count(Query.query(criteria),
            ProductChangeReservation.class);

        return new PageImpl<>(content, pageable, total);
    }

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
    public void markExecuted(String reservationId, LocalDateTime claimedAt) {
        Update update = Update.update("status", ReservationStatus.EXECUTED)
            .set("executedAt", LocalDateTime.now())
            .set("expireAt", LocalDateTime.now().plusDays(EXECUTED_RETENTION_DAYS));

        applyGuardedByProcessing(reservationId, claimedAt, update, "markExecuted");
    }

    @Override
    public boolean markFailed(String reservationId, String failReason, LocalDateTime claimedAt) {
        Update update = Update.update("status", ReservationStatus.FAILED)
            .set("failReason", failReason)
            .set("expireAt", LocalDateTime.now().plusDays(FAILED_RETENTION_DAYS));

        return applyGuardedByProcessing(reservationId, claimedAt, update, "markFailed");
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

    private Criteria buildCriteria(ReservationSearchCondition condition) {
        List<Criteria> conditions = new ArrayList<>();

        if (condition.hasCategory()) {
            List<ReservationCommandType> types = ReservationCommandType
                .ofCategory(condition.category());
            conditions.add(Criteria.where("commandType").in(types));
        }

        if (condition.hasStatus()) {
            conditions.add(Criteria.where("status").is(condition.status()));
        }

        if (conditions.isEmpty()) {
            return new Criteria();
        }
        return new Criteria().andOperator(conditions.toArray(new Criteria[0]));
    }

    private boolean applyGuardedByProcessing(
        String reservationId, LocalDateTime claimedAt, Update update, String operationName) {
        Query query = Query.query(
            Criteria.where("id").is(reservationId)
                .and("status").is(ReservationStatus.PROCESSING)
                .and("claimedAt").is(claimedAt));

        UpdateResult result = mongoTemplate.updateFirst(query, update,
            ProductChangeReservation.class);
        boolean applied = result.getModifiedCount() == 1;

        if (!applied) {
            log.warn("[예약] {} 무시됨 reservationId={}, claimedAt={}",
                operationName, reservationId, claimedAt);
        }

        return applied;
    }
}
