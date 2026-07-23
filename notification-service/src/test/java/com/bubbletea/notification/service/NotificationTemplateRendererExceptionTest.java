package com.bubbletea.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.bubbletea.notification.exception.NotificationErrorCode;
import com.bubbletea.notification.exception.NotificationException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NotificationTemplateRendererExceptionTest {

  private final NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();

  @Nested
  @DisplayName("[EXCEPTION] 알림 템플릿 렌더링")
  class RenderTemplateException {

    @Test
    @DisplayName("템플릿 변수가 없으면 예외가 발생한다")
    void missingVariable() {
      NotificationException exception = catchThrowableOfType(
          NotificationException.class,
          () -> renderer.render("{{nickname}}님 반갑습니다.", Map.of())
      );

      assertThat(exception.getErrorCode())
          .isEqualTo(NotificationErrorCode.NOTIFICATION_TEMPLATE_VARIABLE_NOT_FOUND);
    }

    @Test
    @DisplayName("템플릿 변수 값이 null이면 예외가 발생한다")
    void nullVariable() {
      NotificationException exception = catchThrowableOfType(
          NotificationException.class,
          () -> renderer.render("{{nickname}}님 반갑습니다.",
              java.util.Collections.singletonMap("nickname", null))
      );

      assertThat(exception.getErrorCode())
          .isEqualTo(NotificationErrorCode.NOTIFICATION_TEMPLATE_VARIABLE_NOT_FOUND);
    }
  }
}
