package com.bubbletea.product.infrastructure.mongo.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.product.domain.outbox.OutboxEvent;
import com.bubbletea.product.domain.outbox.OutboxEventRepository;
import com.bubbletea.product.domain.outbox.OutboxEventStatus;
import com.bubbletea.product.support.MongoAuditingTestConfig;
import com.bubbletea.product.support.ProductMongoOnlySupport;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@DataMongoTest
@Import({
    OutboxEventRepositoryImpl.class,
    MongoAuditingTestConfig.class
})
public class OutboxEventRepositoryImplTest extends ProductMongoOnlySupport {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventMongoRepository outboxEventMongoRepository;

    private static final String TOPIC = "notification.product.productRegistered";
    private static final String PAYLOAD = "{\"artistId\":1}";
    private static final Map<String, String> HEADERS = Map.of(
        "domainName", "product",
        "idempotencyKey", "test-key"
    );

    private void setClaimedAt(String id, LocalDateTime claimedAt) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = Update.update("claimedAt", claimedAt);
        mongoTemplate.updateFirst(query, update, OutboxEvent.class);
    }

    private OutboxEvent saveEvent(String idempotencyKey) {
        return outboxEventRepository.save(
            OutboxEvent.of(idempotencyKey, TOPIC, PAYLOAD, HEADERS)
        );
    }

    private OutboxEvent saveAndClaim(String idempotencyKey) {
        saveEvent(idempotencyKey);
        return outboxEventRepository.claimNextPending().orElseThrow();
    }

    @Nested
    @DisplayName("save() 테스트")
    class Save {

        @Test
        @DisplayName("저장한 이벤트를 조회할 수 있다.")
        void saveAndFind_success() {
            // given
            OutboxEvent event = OutboxEvent.of("key-1", TOPIC, PAYLOAD, HEADERS);

            // when
            OutboxEvent saved = outboxEventRepository.save(event);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getIdempotencyKey()).isEqualTo("key-1");
            assertThat(saved.getTopic()).isEqualTo(TOPIC);
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(saved.getRetryCount()).isZero();
        }

        @Test
        @DisplayName("저장 시 createdAt이 자동으로 설정된다.")
        void save_createdAtIsSetAutomatically() {
            // given
            OutboxEvent event = OutboxEvent.of("key-1", TOPIC, PAYLOAD, HEADERS);

            // when
            OutboxEvent saved = outboxEventRepository.save(event);

            // then
            assertThat(saved.getCreatedAt()).isNotNull();
        }

    }

    @Nested
    @DisplayName("claimNextPending() 테스트")
    class ClaimNextPending {

        @Test
        @DisplayName("PENDING 이벤트를 PUBLISHING으로 변경하고 반환한다.")
        void pendingEvent_claimedAsPublishing() {
            // given
            saveEvent("key-1");

            // when
            Optional<OutboxEvent> claimed = outboxEventRepository.claimNextPending();

            // then
            assertThat(claimed).isPresent();
            assertThat(claimed.get().getStatus()).isEqualTo(OutboxEventStatus.PUBLISHING);
            assertThat(claimed.get().getClaimedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING 이벤트가 없으면 빈 Optional을 반환한다.")
        void noPendingEvent_returnsEmpty() {
            // when
            Optional<OutboxEvent> claimed = outboxEventRepository.claimNextPending();

            // then
            assertThat(claimed).isEmpty();
        }

        @Test
        @DisplayName("이미 PUBLISHING 상태인 이벤트는 다시 claim되지 않는다.")
        void publishingEvent_notClaimedAgain() {
            // given
            saveAndClaim("key-1");

            // when
            Optional<OutboxEvent> second = outboxEventRepository.claimNextPending();

            // then
            assertThat(second).isEmpty();
        }

        @Test
        @DisplayName("여러 PENDING 이벤트 중 createdAt이 가장 이른 이벤트를 claim한다.")
        void multiplePendingEvents_claimsEarliestByCreatedAt() throws InterruptedException {
            // given
            saveEvent("key-1");
            Thread.sleep(10);
            saveEvent("key-2");

            // when
            Optional<OutboxEvent> claimed = outboxEventRepository.claimNextPending();

            // then
            assertThat(claimed).isPresent();
            assertThat(claimed.get().getIdempotencyKey()).isEqualTo("key-1");
        }

    }

    @Nested
    @DisplayName("markPublished() 테스트")
    class MarkPublished {

        @Test
        @DisplayName("PUBLISHING 상태의 이벤트를 PUBLISHED로 변경한다.")
        void publishingEvent_markedAsPublished() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");

            // when
            outboxEventRepository.markPublished(claimed.getId(), claimed.getClaimedAt());

            // then
            OutboxEvent updated = outboxEventMongoRepository.findById(claimed.getId())
                .orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
            assertThat(updated.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("markPublished 후 expireAt이 7일 후로 설정된다.")
        void markPublished_setsExpireAtSevenDaysLater() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");
            LocalDateTime beforeMark = LocalDateTime.now();

            // when
            outboxEventRepository.markPublished(claimed.getId(), claimed.getClaimedAt());

            // then
            OutboxEvent updated = outboxEventMongoRepository.findById(claimed.getId())
                .orElseThrow();
            assertThat(updated.getExpireAt())
                .isAfter(beforeMark.plusDays(6))
                .isBefore(beforeMark.plusDays(8));
        }

        @Test
        @DisplayName("[동시성] claimedAt이 다르면 업데이트되지 않는다.")
        void wrongClaimedAt_notUpdated() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");
            LocalDateTime wrongClaimedAt = claimed.getClaimedAt().minusMinutes(1);

            // when
            outboxEventRepository.markPublished(claimed.getId(), wrongClaimedAt);

            // then
            OutboxEvent notUpdated = outboxEventMongoRepository.findById(claimed.getId())
                .orElseThrow();
            assertThat(notUpdated.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHING);
        }

        @Test
        @DisplayName("PENDING 상태의 이벤트는 markPublished로 변경되지 않는다.")
        void pendingEvent_notUpdatedByMarkPublished() {
            // given - claim 없이 PENDING 상태로 저장
            OutboxEvent saved = saveEvent("key-1");

            // when
            outboxEventRepository.markPublished(saved.getId(), LocalDateTime.now());

            // then - PENDING 상태 유지
            OutboxEvent notUpdated = outboxEventMongoRepository.findById(saved.getId())
                .orElseThrow();
            assertThat(notUpdated.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("markFailed() 테스트")
    class MarkFailed {

        @Test
        @DisplayName("PUBLISHING 상태의 이벤트를 FAILED로 변경한다.")
        void publishingEvent_markedAsFailed() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");
            String failReason = "브로커 연결 실패";

            // when
            outboxEventRepository.markFailed(
                claimed.getId(), failReason, claimed.getClaimedAt());

            // then
            OutboxEvent updated = outboxEventMongoRepository.findById(claimed.getId())
                .orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(updated.getFailReason()).isEqualTo(failReason);
        }

        @Test
        @DisplayName("markFailed 후 expireAt이 30일 후로 설정된다.")
        void markFailed_setsExpireAtThirtyDaysLater() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");
            LocalDateTime beforeMark = LocalDateTime.now();

            // when
            outboxEventRepository.markFailed(
                claimed.getId(), "오류", claimed.getClaimedAt());

            // then
            OutboxEvent updated = outboxEventMongoRepository.findById(claimed.getId())
                .orElseThrow();
            assertThat(updated.getExpireAt())
                .isAfter(beforeMark.plusDays(29))
                .isBefore(beforeMark.plusDays(31));
        }

        @Test
        @DisplayName("[동시성] claimedAt이 다르면 업데이트되지 않는다.")
        void wrongClaimedAt_notUpdated() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");
            LocalDateTime wrongClaimedAt = claimed.getClaimedAt().minusMinutes(1);

            // when
            outboxEventRepository.markFailed(claimed.getId(), "오류", wrongClaimedAt);

            // then
            OutboxEvent notUpdated = outboxEventMongoRepository.findById(claimed.getId())
                .orElseThrow();
            assertThat(notUpdated.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHING);
        }
    }

    @Nested
    @DisplayName("markPendingForRetry() 테스트")
    class MarkPendingForRetry {

        @Test
        @DisplayName("PUBLISHING 상태의 이벤트를 PENDING으로 되돌리고 retryCount를 증가시킨다.")
        void publishingEvent_markedAsPendingWithIncreasedRetryCount() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");
            LocalDateTime nextAttemptAt = LocalDateTime.now()
                .plusMinutes(5)
                .truncatedTo(ChronoUnit.MILLIS);

            // when
            outboxEventRepository.markPendingForRetry(
                claimed.getId(), "일시적 오류", nextAttemptAt, claimed.getClaimedAt());

            // then
            OutboxEvent updated = outboxEventMongoRepository.findById(claimed.getId())
                .orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(updated.getRetryCount()).isEqualTo(1);
            assertThat(updated.getFailReason()).isEqualTo("일시적 오류");
            assertThat(updated.getNextAttemptAt()).isEqualTo(nextAttemptAt);
        }

        @Test
        @DisplayName("재시도 후 다시 claimNextPending으로 조회된다.")
        void afterRetry_canBeClaimedAgain() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");
            outboxEventRepository.markPendingForRetry(
                claimed.getId(), "오류", LocalDateTime.now().plusMinutes(5),
                claimed.getClaimedAt()
            );

            // when
            Optional<OutboxEvent> reClaimed = outboxEventRepository.claimNextPending();

            // then
            assertThat(reClaimed).isPresent();
            assertThat(reClaimed.get().getId()).isEqualTo(claimed.getId());
            assertThat(reClaimed.get().getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("[동시성] claimedAt이 다르면 업데이트되지 않는다.")
        void wrongClaimedAt_notUpdated() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");
            LocalDateTime wrongClaimedAt = claimed.getClaimedAt().minusMinutes(1);

            // when
            outboxEventRepository.markPendingForRetry(
                claimed.getId(), "오류",
                LocalDateTime.now().plusMinutes(5), wrongClaimedAt
            );

            // then
            OutboxEvent notUpdated = outboxEventMongoRepository.findById(claimed.getId())
                .orElseThrow();
            assertThat(notUpdated.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHING);
            assertThat(notUpdated.getRetryCount()).isZero();
        }
    }

    @Nested
    @DisplayName("recoverStalledPublishing()")
    class RecoverStalledPublishing {

        @Test
        @DisplayName("staleBefore 이전에 claim된 PUBLISHING 이벤트를 PENDING으로 복구한다")
        void stalledPublishingEvent_recoveredToPending() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");

            LocalDateTime staleClaimedAt = LocalDateTime.now().minusMinutes(10);
            setClaimedAt(claimed.getId(), staleClaimedAt);

            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(5);

            // when
            int recovered = outboxEventRepository.recoverStalledPublishing(staleBefore);

            // then
            assertThat(recovered).isEqualTo(1);
            OutboxEvent updatedEvent =
                outboxEventMongoRepository.findById(claimed.getId()).orElseThrow();
            assertThat(updatedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(updatedEvent.getClaimedAt()).isNull();
        }

        @Test
        @DisplayName("staleBefore 이후에 claim된 PUBLISHING 이벤트는 복구하지 않는다.")
        void recentPublishingEvent_notRecovered() {
            // given
            saveAndClaim("key-1");
            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(5);

            // when
            int recovered = outboxEventRepository.recoverStalledPublishing(staleBefore);

            // then
            assertThat(recovered).isZero();
        }

        @Test
        @DisplayName("PENDING 상태의 이벤트는 복구 대상에서 제외된다.")
        void pendingEvent_notRecovered() {
            // given
            saveEvent("key-1");
            LocalDateTime staleBefore = LocalDateTime.now().plusMinutes(5);

            // when
            int recovered = outboxEventRepository.recoverStalledPublishing(staleBefore);

            // then
            assertThat(recovered).isZero();
        }

        @Test
        @DisplayName("다수의 stalled 이벤트를 모두 복구한다.")
        void multipleStalledEvents_allRecovered() {
            // given
            OutboxEvent claimed1 = saveAndClaim("key-1");
            OutboxEvent claimed2 = saveAndClaim("key-2");

            LocalDateTime staleClaimedAt = LocalDateTime.now().minusMinutes(10);
            setClaimedAt(claimed1.getId(), staleClaimedAt);
            setClaimedAt(claimed2.getId(), staleClaimedAt);

            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(5);

            // when
            int recovered = outboxEventRepository.recoverStalledPublishing(staleBefore);

            // then
            assertThat(recovered).isEqualTo(2);
        }

        @Test
        @DisplayName("복구 후 claimNextPending으로 다시 조회된다.")
        void afterRecovery_canBeClaimedAgain() {
            // given
            OutboxEvent claimed = saveAndClaim("key-1");

            LocalDateTime staleClaimedAt = LocalDateTime.now().minusMinutes(10);
            setClaimedAt(claimed.getId(), staleClaimedAt);

            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(5);
            outboxEventRepository.recoverStalledPublishing(staleBefore);

            // when
            Optional<OutboxEvent> reClaimed = outboxEventRepository.claimNextPending();

            // then
            assertThat(reClaimed).isPresent();
            assertThat(reClaimed.get().getId()).isEqualTo(claimed.getId());
        }
    }

}












