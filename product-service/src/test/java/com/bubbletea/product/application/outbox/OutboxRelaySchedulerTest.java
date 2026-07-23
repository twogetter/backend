package com.bubbletea.product.application.outbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.bubbletea.product.domain.outbox.OutboxEvent;
import com.bubbletea.product.domain.outbox.OutboxEventFixture;
import com.bubbletea.product.domain.outbox.OutboxEventRepository;
import com.bubbletea.product.domain.outbox.OutboxMessageSender;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxRelaySchedulerTest {

    @InjectMocks
    private OutboxRelayScheduler outboxRelayScheduler;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxMessageSender outboxMessageSender;

    // 기본 설정값
    private static final long STALE_THRESHOLD_MINUTES = 5L;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_BASE_BACKOFF_SECONDS = 10L;
    private static final long RETRY_MAX_BACKOFF_SECONDS = 300L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
            outboxRelayScheduler, "stalePublishingThresholdMinutes", STALE_THRESHOLD_MINUTES);
        ReflectionTestUtils.setField(
            outboxRelayScheduler, "maxRetryCount", MAX_RETRY_COUNT);
        ReflectionTestUtils.setField(
            outboxRelayScheduler, "retryBaseBackoffSeconds", RETRY_BASE_BACKOFF_SECONDS);
        ReflectionTestUtils.setField(
            outboxRelayScheduler, "retryMaxBackoffSeconds", RETRY_MAX_BACKOFF_SECONDS);
    }

    @Nested
    @DisplayName("validateConfiguration() 테스트")
    class ValidateConfiguration {

        @Test
        @DisplayName("모든 설정값이 유효하면 예외가 발생하지 않는다.")
        void validConfiguration_doesNotThrow() {
            // when & then
            outboxRelayScheduler.validateConfiguration();
        }

        @Test
        @DisplayName("stalePublishingThresholdMinutes가 음수이면 IllegalStateException을 던진다.")
        void negativeStaleThreshold_throwsIllegalStateException() {
            // given
            ReflectionTestUtils.setField(
                outboxRelayScheduler, "stalePublishingThresholdMinutes", -1L);

            // when & then
            assertThatThrownBy(() -> outboxRelayScheduler.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stale-publishing-threshold-minutes");
        }

        @Test
        @DisplayName("maxRetryCount가 음수이면 IllegalStateException을 던진다.")
        void negativeMaxRetryCount_throwsIllegalStateException() {
            // given
            ReflectionTestUtils.setField(
                outboxRelayScheduler, "maxRetryCount", -1);

            // when & then
            assertThatThrownBy(() -> outboxRelayScheduler.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-retry-count");
        }

        @Test
        @DisplayName("retryBaseBackoffSeconds가 음수이면 IllegalStateException을 던진다.")
        void negativeRetryBaseBackoff_throwsIllegalStateException() {
            // given
            ReflectionTestUtils.setField(
                outboxRelayScheduler, "retryBaseBackoffSeconds", -1L);

            // when & then
            assertThatThrownBy(() -> outboxRelayScheduler.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retry-max-backoff-seconds");
        }

        @Test
        @DisplayName("retryMaxBackoffSeconds가 음수이면 IllegalStateException을 던진다.")
        void negativeRetryMaxBackoff_throwsIllegalStateException() {
            // given
            ReflectionTestUtils.setField(
                outboxRelayScheduler, "retryMaxBackoffSeconds", -1L);

            // when & then
            assertThatThrownBy(() -> outboxRelayScheduler.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retry-max-backoff-seconds");
        }
    }

    @Nested
    @DisplayName("relay() 테스트")
    class Relay {

        @Test
        @DisplayName("pending 이벤트가 없으면 send를 호출하지 않는다.")
        void noPendingEvents_doesNotSend() {
            // given
            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.empty());

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxMessageSender).should(never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("pending 이벤트가 있으면 send를 호출하고 완료 처리한다.")
        void pendingEventExists_sendsAndMarksPublished() {
            // given
            OutboxEvent event = OutboxEventFixture.create();

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event))
                .willReturn(Optional.empty());

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxMessageSender).should().send(
                eq(event.getTopic()),
                eq(event.getPayload()),
                eq(event.getHeaders())
            );
            then(outboxEventRepository).should()
                .markPublished(eq(event.getId()), eq(event.getClaimedAt()));
        }

        @Test
        @DisplayName("pending 이벤트가 여러 개 있으면 모두 발행 처리한다.")
        void multiplePendingEvents_sendsAll() {
            // given
            OutboxEvent event1 = OutboxEventFixture.create("event-1", "topic-1", 0);
            OutboxEvent event2 = OutboxEventFixture.create("event-2", "topic-2", 0);

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event1))
                .willReturn(Optional.of(event2))
                .willReturn(Optional.empty());

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxMessageSender).should(times(2)).send(anyString(), anyString(), any());
            then(outboxEventRepository).should(times(2)).markPublished(any(), any());
        }

        @Test
        @DisplayName("stale 이벤트 복구 후 pending 이벤트를 이어서 처리한다.")
        void recoversStalledThenProcessesPending() {
            // given
            OutboxEvent event = OutboxEventFixture.create();

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(2); // 2건 복구
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event))
                .willReturn(Optional.empty());

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxEventRepository).should()
                .recoverStalledPublishing(any(LocalDateTime.class));
            then(outboxMessageSender).should().send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("stale threshold 기준으로 올바른 시간을 사용해 복구를 시도한다.")
        void recoverUsesCorrectThresholdTime() {
            // given
            LocalDateTime beforeCall = LocalDateTime.now()
                .minusMinutes(STALE_THRESHOLD_MINUTES);

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.empty());

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxEventRepository).should().recoverStalledPublishing(
                argThat(staleBefore ->
                    !staleBefore.isBefore(beforeCall.minusSeconds(1)) &&
                        !staleBefore.isAfter(
                            LocalDateTime.now()
                                .minusMinutes(STALE_THRESHOLD_MINUTES)
                                .plusSeconds(1)
                        )
                )
            );
        }
    }

    @Nested
    @DisplayName("publishOne() 발행 테스트")
    class PublishOne {

        @Test
        @DisplayName("send 성공 시 markPublished를 호출한다.")
        void sendSuccess_callsMarkPublished() {
            // given
            OutboxEvent event = OutboxEventFixture.create();

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event))
                .willReturn(Optional.empty());

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxEventRepository).should()
                .markPublished(eq(event.getId()), eq(event.getClaimedAt()));
            then(outboxEventRepository).should(never())
                .markPendingForRetry(any(), any(), any(), any());
            then(outboxEventRepository).should(never())
                .markFailed(any(), any(), any());
        }

        @Test
        @DisplayName("send 실패 시 retryCount가 maxRetryCount 미만이면 재시도를 예약한다.")
        void sendFails_retryCountBelowMax_scheduleRetry() {
            // given
            // retryCount=0 < maxRetryCount=3
            OutboxEvent event = OutboxEventFixture.createWithRetryCount(0);
            String errorMessage = "connection timeout";

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event))
                .willReturn(Optional.empty());

            willThrow(new RuntimeException(errorMessage))
                .given(outboxMessageSender)
                .send(anyString(), anyString(), any());

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxEventRepository).should().markPendingForRetry(
                eq(event.getId()),
                eq(errorMessage),
                any(LocalDateTime.class),
                eq(event.getClaimedAt())
            );
            then(outboxEventRepository).should(never()).markFailed(any(), any(), any());
            then(outboxEventRepository).should(never()).markPublished(any(), any());
        }

        @Test
        @DisplayName("send 실패 시 retryCount가 maxRetryCount 이상이면 최종 실패 처리한다.")
        void sendFails_retryCountAtMax_marksFailed() {
            // given
            OutboxEvent event = OutboxEventFixture.createWithRetryCount(MAX_RETRY_COUNT);
            String errorMessage = "connection refused";

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event))
                .willReturn(Optional.empty());

            willThrow(new RuntimeException(errorMessage))
                .given(outboxMessageSender)
                .send(anyString(), anyString(), any());

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxEventRepository).should().markFailed(
                eq(event.getId()),
                eq(errorMessage),
                eq(event.getClaimedAt())
            );
            then(outboxEventRepository).should(never())
                .markPendingForRetry(any(), any(), any(), any());
            then(outboxEventRepository).should(never()).markPublished(any(), any());
        }
    }

    @Nested
    @DisplayName("backoffSeconds() 지수 백오프 테스트")
    class BackoffSeconds {

        @Test
        @DisplayName("retryCount=0이면 baseBackoff * 1초를 반환한다.")
        void retryCount0_returnsBaseBackoff() {
            // given
            OutboxEvent event = OutboxEventFixture.createWithRetryCount(0);
            long expectedBackoff = RETRY_BASE_BACKOFF_SECONDS;

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event))
                .willReturn(Optional.empty());

            willThrow(new RuntimeException("error"))
                .given(outboxMessageSender)
                .send(anyString(), anyString(), any());

            LocalDateTime beforeCall = LocalDateTime.now();

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxEventRepository).should().markPendingForRetry(
                eq(event.getId()),
                anyString(),
                argThat(nextAttemptAt ->
                    !nextAttemptAt.isBefore(
                        beforeCall.plusSeconds(expectedBackoff).minusSeconds(1)) &&
                        !nextAttemptAt.isAfter(
                            beforeCall.plusSeconds(expectedBackoff).plusSeconds(1))
                ),
                eq(event.getClaimedAt())
            );
        }

        @Test
        @DisplayName("retryCount=1이면 baseBackoff * 2초를 반환한다.")
        void retryCount1_returnsDoubledBackoff() {
            // given
            OutboxEvent event = OutboxEventFixture.createWithRetryCount(1);
            long expectedBackoff = RETRY_BASE_BACKOFF_SECONDS * 2;

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event))
                .willReturn(Optional.empty());

            willThrow(new RuntimeException("error"))
                .given(outboxMessageSender)
                .send(anyString(), anyString(), any());

            LocalDateTime beforeCall = LocalDateTime.now();

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxEventRepository).should().markPendingForRetry(
                eq(event.getId()),
                anyString(),
                argThat(nextAttemptAt ->
                    !nextAttemptAt.isBefore(
                        beforeCall.plusSeconds(expectedBackoff).minusSeconds(1)) &&
                        !nextAttemptAt.isAfter(
                            beforeCall.plusSeconds(expectedBackoff).plusSeconds(1))
                ),
                eq(event.getClaimedAt())
            );
        }

        @Test
        @DisplayName("계산된 backoff가 maxBackoff를 초과하면 maxBackoff로 제한된다")
        void backoffExceedsMax_cappedAtMaxBackoff() {
            // given
            OutboxEvent event = OutboxEventFixture.createWithRetryCount(1);
            ReflectionTestUtils.setField(
                outboxRelayScheduler, "retryMaxBackoffSeconds", 15L);
            long expectedBackoff = 15L;

            given(outboxEventRepository.recoverStalledPublishing(any(LocalDateTime.class)))
                .willReturn(0);
            given(outboxEventRepository.claimNextPending())
                .willReturn(Optional.of(event))
                .willReturn(Optional.empty());

            willThrow(new RuntimeException("error"))
                .given(outboxMessageSender)
                .send(anyString(), anyString(), any());

            LocalDateTime beforeCall = LocalDateTime.now();

            // when
            outboxRelayScheduler.relay();

            // then
            then(outboxEventRepository).should().markPendingForRetry(
                eq(event.getId()),
                anyString(),
                argThat(nextAttemptAt ->
                    !nextAttemptAt.isBefore(
                        beforeCall.plusSeconds(expectedBackoff).minusSeconds(1)) &&
                        !nextAttemptAt.isAfter(
                            beforeCall.plusSeconds(expectedBackoff).plusSeconds(1))
                ),
                eq(event.getClaimedAt())
            );
        }
    }
}