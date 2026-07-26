package com.bubbletea.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NotificationTemplateRendererHappyTest {

  private final NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();

  @Nested
  @DisplayName("[HAPPY] 알림 템플릿 렌더링")
  class RenderTemplate {

    @Test
    @DisplayName("공백이 포함된 여러 템플릿 변수를 치환한다")
    void renderVariables() {
      String result = renderer.render(
          "{{ nickname }}님의 결제 금액은 {{amount}}원입니다.",
          Map.of("nickname", "버블티", "amount", 12000)
      );

      assertThat(result).isEqualTo("버블티님의 결제 금액은 12000원입니다.");
    }

    @Test
    @DisplayName("치환 값의 정규식 특수 문자를 그대로 렌더링한다")
    void renderRegexReplacementCharacters() {
      String result = renderer.render("메시지: {{message}}", Map.of("message", "$1\\완료"));

      assertThat(result).isEqualTo("메시지: $1\\완료");
    }

    @Test
    @DisplayName("변수가 없는 템플릿은 변수 맵이 null이어도 그대로 반환한다")
    void renderWithoutVariables() {
      assertThat(renderer.render("새 알림이 도착했습니다.", null))
          .isEqualTo("새 알림이 도착했습니다.");
    }
  }
}
