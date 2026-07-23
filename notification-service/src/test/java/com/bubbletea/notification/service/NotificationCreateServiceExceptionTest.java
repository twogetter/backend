package com.bubbletea.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.exception.NotificationErrorCode;
import com.bubbletea.notification.exception.NotificationException;
import com.bubbletea.notification.repository.NotificationRepository;
import com.bubbletea.notification.repository.NotificationTemplateRepository;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class NotificationCreateServiceExceptionTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private NotificationTemplateRepository notificationTemplateRepository;

  @Mock
  private NotificationTemplateRenderer notificationTemplateRenderer;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private TransactionOperations transactionOperations;

  @InjectMocks
  private NotificationCreateService notificationCreateService;

  @Nested
  @DisplayName("[EXCEPTION] 알림 생성")
  class CreateNotificationException {

    @Test
    @DisplayName("알림 템플릿이 없으면 예외가 발생한다")
    void templateNotFound() {
      executeTransactionCallback();
      NotificationCreateCommandDto command = command();
      given(notificationRepository.existsByEventIdAndReceiverId("event-1", 1L))
          .willReturn(false);
      given(notificationTemplateRepository.findByNotificationType("MEMBER_SIGNED_UP"))
          .willReturn(Optional.empty());

      NotificationException exception = catchThrowableOfType(
          NotificationException.class,
          () -> notificationCreateService.create(command)
      );

      assertThat(exception.getErrorCode())
          .isEqualTo(NotificationErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);
      then(notificationRepository).should(never()).saveAndFlush(any());
      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("동시 요청으로 같은 알림의 유니크 키가 충돌하면 중복으로 처리한다")
    void skipUniqueConstraintViolation() {
      DataIntegrityViolationException exception = dataIntegrityViolation(
          "uk_notifications_event_receiver"
      );
      doThrow(exception).when(transactionOperations).executeWithoutResult(any());

      assertThatCode(() -> notificationCreateService.create(command())).doesNotThrowAnyException();

      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("알림 중복과 무관한 무결성 예외는 다시 던진다")
    void rethrowOtherDataIntegrityViolation() {
      DataIntegrityViolationException exception = dataIntegrityViolation("other_constraint");
      doThrow(exception).when(transactionOperations).executeWithoutResult(any());

      assertThatThrownBy(() -> notificationCreateService.create(command())).isSameAs(exception);
    }
  }

  private void executeTransactionCallback() {
    doAnswer(invocation -> {
      Consumer<TransactionStatus> callback = invocation.getArgument(0);
      callback.accept(mock(TransactionStatus.class));
      return null;
    }).when(transactionOperations).executeWithoutResult(any());
  }

  private DataIntegrityViolationException dataIntegrityViolation(String constraintName) {
    ConstraintViolationException cause = new ConstraintViolationException(
        "constraint violation",
        new SQLException("constraint violation"),
        constraintName
    );
    return new DataIntegrityViolationException("save failed", cause);
  }

  private NotificationCreateCommandDto command() {
    return new NotificationCreateCommandDto(
        "event-1",
        1L,
        "MEMBER_SIGNED_UP",
        Map.of("nickname", "버블티"),
        null,
        Map.of("nickname", "버블티")
    );
  }
}
