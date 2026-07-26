package com.bubbletea.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bubbletea.notification.exception.NotificationErrorCode;
import com.bubbletea.notification.exception.NotificationException;
import com.bubbletea.notification.repository.NotificationRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceExceptionTest {

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationService notificationService;

  @Nested
  @DisplayName("[EXCEPTION] 알림 목록 조회")
  class GetNotificationsException {

    @ParameterizedTest(name = "receiverId={0}")
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("수신자 ID가 유효하지 않으면 예외가 발생한다")
    void invalidReceiverId(Long receiverId) {
      NotificationException exception = catchThrowableOfType(
          NotificationException.class,
          () -> notificationService.getNotifications(receiverId, 0, 20)
      );

      assertThat(exception.getErrorCode()).isEqualTo(NotificationErrorCode.INVALID_RECEIVER_ID);
      then(notificationRepository).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "page={0}, size={1}")
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    @DisplayName("페이지 요청 값이 범위를 벗어나면 예외가 발생한다")
    void invalidPageRequest(int page, int size) {
      NotificationException exception = catchThrowableOfType(
          NotificationException.class,
          () -> notificationService.getNotifications(1L, page, size)
      );

      assertThat(exception.getErrorCode()).isEqualTo(NotificationErrorCode.INVALID_PAGE_REQUEST);
      then(notificationRepository).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("[EXCEPTION] 알림 읽음 처리")
  class ReadNotificationException {

    @ParameterizedTest(name = "receiverId={0}")
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("수신자 ID가 유효하지 않으면 예외가 발생한다")
    void invalidReceiverId(Long receiverId) {
      NotificationException exception = catchThrowableOfType(
          NotificationException.class,
          () -> notificationService.readNotification(10L, receiverId)
      );

      assertThat(exception.getErrorCode()).isEqualTo(NotificationErrorCode.INVALID_RECEIVER_ID);
      then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("수신자에게 속한 알림이 없으면 예외가 발생한다")
    void notificationNotFound() {
      given(notificationRepository.findByIdAndReceiverId(10L, 1L))
          .willReturn(Optional.empty());

      NotificationException exception = catchThrowableOfType(
          NotificationException.class,
          () -> notificationService.readNotification(10L, 1L)
      );

      assertThat(exception.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }
  }
}
