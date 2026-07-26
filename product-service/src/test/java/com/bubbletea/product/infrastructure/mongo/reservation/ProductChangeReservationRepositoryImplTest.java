package com.bubbletea.product.infrastructure.mongo.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationFixture;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import com.bubbletea.product.domain.reservation.ReservationSearchCondition;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import com.bubbletea.product.support.MongoAuditingTestConfig;
import com.bubbletea.product.support.ProductMongoOnlySupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;


@DataMongoTest
@Import({
    ProductChangeReservationRepositoryImpl.class,
    MongoAuditingTestConfig.class
})
public class ProductChangeReservationRepositoryImplTest extends ProductMongoOnlySupport {

    @Autowired
    private ProductChangeReservationRepository reservationRepository;

    @Autowired
    private ProductChangeReservationMongoRepository mongoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private ProductChangeReservation savePendingReservation(
        ReservationCommandType commandType, LocalDateTime scheduledAt
    ) {
        ProductChangeReservation reservation =
            ProductChangeReservationFixture.createByCommandType(commandType, scheduledAt);
        return mongoRepository.save(reservation);
    }

    private ProductChangeReservation saveAndClaim(
        ReservationCommandType commandType, LocalDateTime scheduledAt
    ) {
        savePendingReservation(commandType, scheduledAt);
        LocalDateTime now = LocalDateTime.now().plusSeconds(1);
        return reservationRepository.claimNextPending(now, List.of(commandType))
            .orElseThrow();
    }

    private void setClaimedAt(String id, LocalDateTime claimedAt) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = Update.update("claimedAt", claimedAt);
        mongoTemplate.updateFirst(query, update, ProductChangeReservation.class);
    }

    private ProductChangeReservation findById(String id) {
        return mongoRepository.findById(id)
            .orElseThrow(() -> new AssertionError("예약을 찾을 수 없음: " + id));
    }

    @Nested
    @DisplayName("findById() 테스트")
    class FindById {

        @Test
        @DisplayName("존재하는 reservationId로 조회하면 예약을 성공적으로 반환한다.")
        void existingId_returnsReservation() {
            // given
            ProductChangeReservation saved = savePendingReservation(
                ReservationCommandType.ACTIVATE,
                LocalDateTime.now().plusDays(1)
            );

            // when
            Optional<ProductChangeReservation> found =
                reservationRepository.findById(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getCommandType()).isEqualTo(ReservationCommandType.ACTIVATE);
        }

        @Test
        @DisplayName("존재하지 않는 reservationId로 조회하면 빈 Optional을 반환한다.")
        void nonExistingId_returnsEmpty() {
            // when
            Optional<ProductChangeReservation> found =
                reservationRepository.findById("not-existing-id");

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll() 페이징 조회 테스트")
    class FindAll {

        @Test
        @DisplayName("조건 없이 조회하면 전체 예약을 반환한다.")
        void noCondition_returnsAllReservations() {
            // given
            savePendingReservation(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().plusDays(1));
            savePendingReservation(
                ReservationCommandType.DELETION, LocalDateTime.now().plusDays(2));

            ReservationSearchCondition condition =
                new ReservationSearchCondition(null, null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<ProductChangeReservation> result =
                reservationRepository.findAll(condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("category 조건으로 필터링하면 해당 카테고리 예약만 반환한다.")
        void categoryFilter_returnsFilteredReservations() {
            // given
            savePendingReservation(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().plusDays(1));
            savePendingReservation(
                ReservationCommandType.NOTIFY_OPEN_SCHEDULE, LocalDateTime.now().plusDays(1));
            savePendingReservation(
                ReservationCommandType.NOTIFY_DELETION_SCHEDULE, LocalDateTime.now().plusDays(1));

            ReservationSearchCondition condition =
                new ReservationSearchCondition(ReservationCategory.STATUS_CHANGE, null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<ProductChangeReservation> result =
                reservationRepository.findAll(condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getCommandType())
                .isEqualTo(ReservationCommandType.ACTIVATE);
        }

        @Test
        @DisplayName("status 조건으로 필터링하면 해당 상태의 예약만 반환한다.")
        void statusFilter_returnsFilteredReservations() {
            // given
            savePendingReservation(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            saveAndClaim(
                ReservationCommandType.DELETION, LocalDateTime.now().minusMinutes(1));

            ReservationSearchCondition condition =
                new ReservationSearchCondition(null, ReservationStatus.PENDING);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<ProductChangeReservation> result =
                reservationRepository.findAll(condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getStatus())
                .isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("category와 status 조건을 함께 사용하면 복합 조건으로 필터링된다.")
        void categoryAndStatusFilter_returnsFilteredReservations() {
            // given
            savePendingReservation(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            savePendingReservation(
                ReservationCommandType.NOTIFY_OPEN_SCHEDULE, LocalDateTime.now().minusMinutes(1));
            saveAndClaim(
                ReservationCommandType.DELETION, LocalDateTime.now().minusMinutes(1));

            ReservationSearchCondition condition = new ReservationSearchCondition(
                ReservationCategory.STATUS_CHANGE, ReservationStatus.PENDING);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<ProductChangeReservation> result =
                reservationRepository.findAll(condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getCommandType())
                .isEqualTo(ReservationCommandType.ACTIVATE);
            assertThat(result.getContent().getFirst().getStatus())
                .isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("페이징이 정상적으로 적용된다.")
        void pagingApplied_whenDataExceedsPageSize() {
            // given
            for (int i = 0; i < 5; i++) {
                savePendingReservation(
                    ReservationCommandType.ACTIVATE, LocalDateTime.now().plusDays(i + 1));
            }

            ReservationSearchCondition condition =
                new ReservationSearchCondition(null, null);
            Pageable pageable = PageRequest.of(0, 3);

            // when
            Page<ProductChangeReservation> result =
                reservationRepository.findAll(condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("조건에 맞는 예약이 없으면 빈 페이지를 반환한다.")
        void noMatchingReservations_returnsEmptyPage() {
            // given
            ReservationSearchCondition condition =
                new ReservationSearchCondition(ReservationCategory.PRICE_CHANGE, null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<ProductChangeReservation> result =
                reservationRepository.findAll(condition, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("existsByProductIdAndReservationStatus() 테스트")
    class ExistsByProductIdAndReservationStatus {

        @Test
        @DisplayName("조건에 맞는 예약이 있으면 true를 반환한다.")
        void existingReservation_returnsTrue() {
            // given
            ProductChangeReservation reservation =
                ProductChangeReservationFixture.deletion(
                    "product-1", LocalDateTime.now().plusDays(1));
            reservationRepository.saveAll(List.of(reservation));

            // when
            boolean result = reservationRepository.existsByProductIdAndReservationStatus(
                "product-1", ReservationStatus.PENDING);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("productId와 일치하는 예약이 없으면 false를 반환한다.")
        void noReservation_returnsFalse() {
            // when
            boolean result = reservationRepository.existsByProductIdAndReservationStatus(
                "not-existing", ReservationStatus.PENDING);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("예약은 있지만 status가 다르면 false를 반환한다.")
        void differentStatus_returnsFalse() {
            // given
            saveAndClaim(
                ReservationCommandType.ACTIVATE,
                LocalDateTime.now().minusMinutes(1)
            );

            // when
            boolean result = reservationRepository.existsByProductIdAndReservationStatus(
                ProductChangeReservationFixture.DEFAULT_PRODUCT_ID, ReservationStatus.PENDING);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("claimNextPending() 테스트")
    class ClaimNextPending {

        @Test
        @DisplayName("scheduledAt이 now 이전인 PENDING 예약을 PROCESSING으로 변경하고 반환한다.")
        void pendingReservationDue_claimedAsProcessing() {
            // given
            savePendingReservation(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            LocalDateTime now = LocalDateTime.now();

            // when
            Optional<ProductChangeReservation> claimed = reservationRepository
                .claimNextPending(now, List.of(ReservationCommandType.ACTIVATE));

            // then
            assertThat(claimed).isPresent();
            assertThat(claimed.get().getStatus()).isEqualTo(ReservationStatus.PROCESSING);
            assertThat(claimed.get().getClaimedAt()).isNotNull();
        }

        @Test
        @DisplayName("scheduledAt이 now 이후인 예약은 claim되지 않는다.")
        void futureReservation_notClaimed() {
            // given
            savePendingReservation(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().plusHours(1));
            LocalDateTime now = LocalDateTime.now();

            // when
            Optional<ProductChangeReservation> claimed = reservationRepository
                .claimNextPending(now, List.of(ReservationCommandType.ACTIVATE));

            // then
            assertThat(claimed).isEmpty();
        }

        @Test
        @DisplayName("commandType이 일치하지 않으면 claim되지 않는다.")
        void differentCommandType_notClaimed() {
            // given
            savePendingReservation(
                ReservationCommandType.DELETION, LocalDateTime.now().minusMinutes(1));
            LocalDateTime now = LocalDateTime.now();

            // when
            Optional<ProductChangeReservation> claimed = reservationRepository
                .claimNextPending(now, List.of(ReservationCommandType.ACTIVATE));

            // then
            assertThat(claimed).isEmpty();
        }

        @Test
        @DisplayName("여러건일 때 scheduledAt이 가장 이른 예약을 claim한다.")
        void multipleCommandTypes_claimsEarliestScheduledAt() {
            // given
            LocalDateTime earlier = LocalDateTime.now().minusMinutes(10);
            LocalDateTime later = LocalDateTime.now().minusMinutes(5);

            savePendingReservation(ReservationCommandType.DELETION, later);
            savePendingReservation(ReservationCommandType.ACTIVATE, earlier);

            LocalDateTime now = LocalDateTime.now();

            // when
            Optional<ProductChangeReservation> claimed = reservationRepository
                .claimNextPending(now, List.of(ReservationCommandType.ACTIVATE,
                    ReservationCommandType.DELETION));

            // then
            assertThat(claimed).isPresent();
            assertThat(claimed.get().getCommandType()).isEqualTo(ReservationCommandType.ACTIVATE);
        }

        @Test
        @DisplayName("PROCESSING 상태의 예약은 다시 claim되지 않는다.")
        void processingReservation_notClaimedAgain() {
            // given
            saveAndClaim(ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            LocalDateTime now = LocalDateTime.now();

            // when
            Optional<ProductChangeReservation> second = reservationRepository
                .claimNextPending(now, List.of(ReservationCommandType.ACTIVATE));

            // then
            assertThat(second).isEmpty();
        }

        @Test
        @DisplayName("PENDING 예약이 없으면 빈 Optional을 반환한다.")
        void noPendingReservation_returnsEmpty() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            Optional<ProductChangeReservation> claimed = reservationRepository
                .claimNextPending(now, List.of(ReservationCommandType.ACTIVATE));

            // then
            assertThat(claimed).isEmpty();
        }
    }

    @Nested
    @DisplayName("markExecuted() 테스트")
    class MarkExecuted {

        @Test
        @DisplayName("PROCESSING 상태의 예약을 EXECUTED로 변경한다.")
        void processingReservation_markedAsExecuted() {
            // given
            ProductChangeReservation claimed = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));

            // when
            reservationRepository.markExecuted(claimed.getId(), claimed.getClaimedAt());

            // then
            ProductChangeReservation updated = findById(claimed.getId());
            assertThat(updated.getStatus()).isEqualTo(ReservationStatus.EXECUTED);
            assertThat(updated.getExecutedAt()).isNotNull();
        }

        @Test
        @DisplayName("markExecuted 후 expireAt이 7일 후로 설정된다.")
        void markExecuted_setsExpireAtSevenDaysLater() {
            // given
            ProductChangeReservation claimed = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            LocalDateTime beforeMark = LocalDateTime.now();

            // when
            reservationRepository.markExecuted(claimed.getId(), claimed.getClaimedAt());

            // then
            ProductChangeReservation updated = findById(claimed.getId());
            assertThat(updated.getExpireAt())
                .isAfter(beforeMark.plusDays(6))
                .isBefore(beforeMark.plusDays(8));
        }

        @Test
        @DisplayName("[동시성] claimedAt이 다르면 업데이트되지 않는다.")
        void wrongClaimedAt_notUpdated() {
            // given
            ProductChangeReservation claimed = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            LocalDateTime wrongClaimedAt = claimed.getClaimedAt().minusMinutes(1);

            // when
            reservationRepository.markExecuted(claimed.getId(), wrongClaimedAt);

            // then
            ProductChangeReservation notUpdated = findById(claimed.getId());
            assertThat(notUpdated.getStatus()).isEqualTo(ReservationStatus.PROCESSING);
        }
    }

    @Nested
    @DisplayName("markFailed() 테스트")
    class MarkFailed {

        @Test
        @DisplayName("PROCESSING 상태의 예약을 FAILED로 변경하고 true를 반환한다.")
        void processingReservation_markedAsFailedAndReturnsTrue() {
            // given
            ProductChangeReservation claimed = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            String failReason = "실행 중 오류 발생";

            // when
            boolean result = reservationRepository.markFailed(
                claimed.getId(), failReason, claimed.getClaimedAt());

            // then
            assertThat(result).isTrue();
            ProductChangeReservation updated = findById(claimed.getId());
            assertThat(updated.getStatus()).isEqualTo(ReservationStatus.FAILED);
            assertThat(updated.getFailReason()).isEqualTo(failReason);
        }

        @Test
        @DisplayName("markFailed 후 expireAt이 30일 후로 설정된다.")
        void markFailed_setsExpireAtThirtyDaysLater() {
            // given
            ProductChangeReservation claimed = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            LocalDateTime beforeMark = LocalDateTime.now();

            // when
            reservationRepository.markFailed(
                claimed.getId(), "오류", claimed.getClaimedAt());

            // then
            ProductChangeReservation updated = findById(claimed.getId());
            assertThat(updated.getExpireAt())
                .isAfter(beforeMark.plusDays(29))
                .isBefore(beforeMark.plusDays(31));
        }

        @Test
        @DisplayName("claimedAt이 다르면 업데이트되지 않고 false를 반환한다.")
        void wrongClaimedAt_notUpdatedAndReturnsFalse() {
            // given
            ProductChangeReservation claimed = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            LocalDateTime wrongClaimedAt = claimed.getClaimedAt().minusMinutes(1);

            // when
            boolean result = reservationRepository.markFailed(
                claimed.getId(), "오류", wrongClaimedAt);

            // then
            assertThat(result).isFalse();
            ProductChangeReservation notUpdated = findById(claimed.getId());
            assertThat(notUpdated.getStatus()).isEqualTo(ReservationStatus.PROCESSING);
        }
    }

    @Nested
    @DisplayName("recoverStalledProcessing() 테스트")
    class RecoverStalledProcessing {

        @Test
        @DisplayName("staleBefore 이전에 claim된 PROCESSING 예약을 PENDING으로 복구한다.")
        void stalledProcessingReservation_recoveredToPending() {
            // given
            ProductChangeReservation claimed = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));

            LocalDateTime staleClaimedAt = LocalDateTime.now().minusMinutes(15);
            setClaimedAt(claimed.getId(), staleClaimedAt);

            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(10);

            // when
            int recovered = reservationRepository.recoverStalledProcessing(staleBefore);

            // then
            assertThat(recovered).isEqualTo(1);
            ProductChangeReservation updated = findById(claimed.getId());
            assertThat(updated.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(updated.getClaimedAt()).isNull();
        }

        @Test
        @DisplayName("staleBefore 이후에 claim된 PROCESSING 예약은 복구하지 않는다.")
        void recentProcessingReservation_notRecovered() {
            // given
            saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(10);

            // when
            int recovered = reservationRepository.recoverStalledProcessing(staleBefore);

            // then
            assertThat(recovered).isZero();
        }

        @Test
        @DisplayName("PENDING 상태의 예약은 복구 대상에서 제외된다.")
        void pendingReservation_notRecovered() {
            // given
            savePendingReservation(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));
            LocalDateTime staleBefore = LocalDateTime.now().plusMinutes(5);

            // when
            int recovered = reservationRepository.recoverStalledProcessing(staleBefore);

            // then
            assertThat(recovered).isZero();
        }

        @Test
        @DisplayName("stalled 예약이 여러 개이면 모두 복구한다.")
        void multipleStalledReservations_allRecovered() {
            // given
            ProductChangeReservation claimed1 = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(2));
            ProductChangeReservation claimed2 = saveAndClaim(
                ReservationCommandType.DELETION, LocalDateTime.now().minusMinutes(2));

            LocalDateTime staleClaimedAt = LocalDateTime.now().minusMinutes(15);
            setClaimedAt(claimed1.getId(), staleClaimedAt);
            setClaimedAt(claimed2.getId(), staleClaimedAt);

            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(10);

            // when
            int recovered = reservationRepository.recoverStalledProcessing(staleBefore);

            // then
            assertThat(recovered).isEqualTo(2);
        }

        @Test
        @DisplayName("복구 후 claimNextPending으로 다시 조회된다.")
        void afterRecovery_canBeClaimedAgain() {
            // given
            ProductChangeReservation claimed = saveAndClaim(
                ReservationCommandType.ACTIVATE, LocalDateTime.now().minusMinutes(1));

            LocalDateTime staleClaimedAt = LocalDateTime.now().minusMinutes(15);
            setClaimedAt(claimed.getId(), staleClaimedAt);

            reservationRepository.recoverStalledProcessing(
                LocalDateTime.now().minusMinutes(10));

            // when
            Optional<ProductChangeReservation> reClaimed = reservationRepository
                .claimNextPending(LocalDateTime.now(), List.of(ReservationCommandType.ACTIVATE));

            // then
            assertThat(reClaimed).isPresent();
            assertThat(reClaimed.get().getId()).isEqualTo(claimed.getId());
        }
    }

}
