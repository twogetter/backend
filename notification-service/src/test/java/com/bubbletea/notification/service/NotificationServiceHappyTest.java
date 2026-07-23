package com.bubbletea.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bubbletea.notification.entity.Notification;
import com.bubbletea.notification.repository.NotificationRepository;
import com.bubbletea.notification.service.dto.NotificationListResponseDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceHappyTest {

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationService notificationService;

  @Nested
  @DisplayName("[HAPPY] 알림 목록 조회")
  class GetNotifications {

    @Test
    @DisplayName("수신자의 알림을 최신 생성순과 ID 역순으로 조회한다")
    void getNotifications() {
      Notification notification = notification(10L, 1L);
      PageRequest repositoryPage = PageRequest.of(0, 20);
      given(notificationRepository.findAllByReceiverId(eq(1L), any(Pageable.class)))
          .willReturn(new SliceImpl<>(List.of(notification), repositoryPage, false));

      NotificationListResponseDto response = notificationService.getNotifications(1L, 0, 20);

      assertThat(response.notifications()).hasSize(1);
      assertThat(response.notifications().getFirst().id()).isEqualTo(10L);
      assertThat(response.page()).isZero();
      assertThat(response.size()).isEqualTo(20);
      assertThat(response.hasNext()).isFalse();

      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      then(notificationRepository).should()
          .findAllByReceiverId(eq(1L), pageableCaptor.capture());

      Pageable pageable = pageableCaptor.getValue();
      assertThat(pageable.getPageNumber()).isZero();
      assertThat(pageable.getPageSize()).isEqualTo(20);
      assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
          .isEqualTo(Sort.Direction.DESC);
      assertThat(pageable.getSort().getOrderFor("id").getDirection())
          .isEqualTo(Sort.Direction.DESC);
    }
  }

  @Nested
  @DisplayName("[HAPPY] 알림 읽음 처리")
  class ReadNotification {

    @Test
    @DisplayName("수신자에게 속한 알림을 읽음 처리한다")
    void readNotification() {
      Notification notification = notification(10L, 1L);
      given(notificationRepository.findByIdAndReceiverId(10L, 1L))
          .willReturn(Optional.of(notification));
      LocalDateTime beforeRead = LocalDateTime.now();

      notificationService.readNotification(10L, 1L);

      assertThat(notification.getReadAt()).isAfterOrEqualTo(beforeRead);
      then(notificationRepository).should().findByIdAndReceiverId(10L, 1L);
    }

    @Test
    @DisplayName("이미 읽은 알림은 최초 읽음 시각을 유지한다")
    void keepFirstReadAt() {
      Notification notification = notification(10L, 1L);
      LocalDateTime firstReadAt = LocalDateTime.of(2026, 7, 20, 10, 0);
      notification.read(firstReadAt);
      given(notificationRepository.findByIdAndReceiverId(10L, 1L))
          .willReturn(Optional.of(notification));

      notificationService.readNotification(10L, 1L);

      assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }
  }

  private Notification notification(Long id, Long receiverId) {
    Notification notification = Notification.builder()
        .receiverId(receiverId)
        .eventId("event-1")
        .notificationType("MEMBER_SIGNED_UP")
        .title("가입 완료")
        .contents("가입을 환영합니다.")
        .linkUrl("/members/1")
        .payload(Map.of("nickname", "버블티"))
        .build();
    ReflectionTestUtils.setField(notification, "id", id);
    return notification;
  }
}
