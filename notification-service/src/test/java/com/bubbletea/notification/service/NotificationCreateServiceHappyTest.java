package com.bubbletea.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.entity.Notification;
import com.bubbletea.notification.entity.NotificationTemplate;
import com.bubbletea.notification.repository.NotificationRepository;
import com.bubbletea.notification.repository.NotificationTemplateRepository;
import com.bubbletea.notification.service.event.NotificationCreatedEvent;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class NotificationCreateServiceHappyTest {

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

  @BeforeEach
  void executeTransactionCallback() {
    doAnswer(invocation -> {
      Consumer<TransactionStatus> callback = invocation.getArgument(0);
      callback.accept(mock(TransactionStatus.class));
      return null;
    }).when(transactionOperations).executeWithoutResult(any());
  }

  @Nested
  @DisplayName("[HAPPY] 알림 생성")
  class CreateNotification {

    @Test
    @DisplayName("템플릿을 렌더링해 알림을 저장하고 생성 이벤트를 발행한다")
    void createAndPublishEvent() {
      NotificationCreateCommandDto command = command();
      NotificationTemplate template = NotificationTemplate.builder()
          .notificationType(command.notificationType())
          .titleTemplate("{{nickname}}님, 가입 완료")
          .contentsTemplate("{{nickname}}님의 가입을 환영합니다.")
          .variables(Map.of("nickname", "String"))
          .build();
      given(notificationRepository.existsByEventIdAndReceiverId("event-1", 1L))
          .willReturn(false);
      given(notificationTemplateRepository.findByNotificationType("MEMBER_SIGNED_UP"))
          .willReturn(Optional.of(template));
      given(notificationTemplateRenderer.render(template.getTitleTemplate(), command.variables()))
          .willReturn("버블티님, 가입 완료");
      given(notificationTemplateRenderer.render(template.getContentsTemplate(), command.variables()))
          .willReturn("버블티님의 가입을 환영합니다.");
      given(notificationRepository.saveAndFlush(any(Notification.class)))
          .willAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
          });

      notificationCreateService.create(command);

      ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
      then(notificationRepository).should().saveAndFlush(notificationCaptor.capture());
      Notification saved = notificationCaptor.getValue();
      assertThat(saved.getReceiverId()).isEqualTo(1L);
      assertThat(saved.getEventId()).isEqualTo("event-1");
      assertThat(saved.getTitle()).isEqualTo("버블티님, 가입 완료");
      assertThat(saved.getContents()).isEqualTo("버블티님의 가입을 환영합니다.");
      assertThat(saved.getPayload()).containsEntry("nickname", "버블티");

      ArgumentCaptor<NotificationCreatedEvent> eventCaptor =
          ArgumentCaptor.forClass(NotificationCreatedEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());
      assertThat(eventCaptor.getValue().receiverId()).isEqualTo(1L);
      assertThat(eventCaptor.getValue().notification().id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("같은 이벤트와 수신자의 알림이 이미 있으면 생성을 건너뛴다")
    void skipDuplicatedNotification() {
      NotificationCreateCommandDto command = command();
      given(notificationRepository.existsByEventIdAndReceiverId("event-1", 1L))
          .willReturn(true);

      notificationCreateService.create(command);

      then(notificationTemplateRepository).shouldHaveNoInteractions();
      then(notificationRepository).should(never()).saveAndFlush(any(Notification.class));
      verifyNoInteractions(notificationTemplateRenderer, eventPublisher);
    }
  }

  private NotificationCreateCommandDto command() {
    return new NotificationCreateCommandDto(
        "event-1",
        1L,
        "MEMBER_SIGNED_UP",
        Map.of("nickname", "버블티"),
        "/members/1",
        Map.of("nickname", "버블티")
    );
  }
}
