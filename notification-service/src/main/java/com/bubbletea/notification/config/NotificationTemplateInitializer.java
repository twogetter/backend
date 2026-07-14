package com.bubbletea.notification.config;

import com.bubbletea.notification.entity.NotificationTemplate;
import com.bubbletea.notification.entity.enums.NotificationType;
import com.bubbletea.notification.repository.NotificationTemplateRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class NotificationTemplateInitializer {

  private final NotificationTemplateRepository notificationTemplateRepository;

  @Bean
  ApplicationRunner initializeNotificationTemplates() {
    return arguments -> {
      List<NotificationTemplate> templates = templates();
      int createdCount = 0;

      for (NotificationTemplate template : templates) {
        if (notificationTemplateRepository.findByNotificationType(
            template.getNotificationType()
        ).isEmpty()) {
          notificationTemplateRepository.save(template);
          createdCount++;
        }
      }

      log.info("개발용 알림 템플릿 초기화 완료. createdCount={}", createdCount);
    };
  }

  private List<NotificationTemplate> templates() {
    return List.of(
        template(
            NotificationType.PRODUCT_ACTIVATION_SCHEDULED,
            "상품 활성화 예정",
            "{{productName}} 상품이 {{schedule}}부터 다시 활성화됩니다.",
            Map.of("productName", "상품명", "schedule", "활성화 예정일")
        ),
        template(
            NotificationType.PRODUCT_DELETION_SCHEDULED,
            "상품 삭제 예정",
            "{{productName}} 상품이 {{date}}부터 영구적으로 삭제됩니다.",
            Map.of("productName", "상품명", "date", "삭제 예정일")
        ),
        template(
            NotificationType.PRODUCT_PRICE_CHANGE_SCHEDULED,
            "상품 가격 변경 예정",
            "{{productName}} 상품의 가격이 {{date}}부터 변경됩니다. 기존 가격은 "
                + "{{beforePrice}}원, 변경 가격은 {{afterPrice}}원입니다.",
            Map.of(
                "productName", "상품명",
                "date", "가격 변경일",
                "beforePrice", "기존 가격",
                "afterPrice", "변경 가격"
            )
        ),
        template(
            NotificationType.PRODUCT_DEACTIVATION_SCHEDULED,
            "상품 비활성화 예정",
            "{{productName}} 상품이 {{suspendDate}}부터 비활성화됩니다.",
            Map.of("productName", "상품명", "suspendDate", "비활성화 예정일")
        ),
        template(
            NotificationType.PRODUCT_OPEN_SCHEDULED,
            "상품 오픈 예정",
            "{{productName}} 상품이 {{schedule}}부터 새롭게 오픈됩니다.",
            Map.of("productName", "상품명", "schedule", "오픈 예정일")
        ),
        template(
            NotificationType.PAYMENT_COMPLETE,
            "결제가 완료되었습니다.",
            "{{amount}}원 결제가 완료되었습니다.",
            Map.of("amount", "결제 금액")
        ),
        template(
            NotificationType.PAYMENT_FAIL,
            "결제에 실패했습니다.",
            "{{amount}}원 결제에 실패했습니다. 결제 정보를 확인해주세요.",
            Map.of("amount", "결제 금액")
        ),
        template(
            NotificationType.MEMBER_SIGNED_UP,
            "회원가입을 환영합니다.",
            "{{nickname}}님의 회원가입이 완료되었습니다.",
            Map.of("nickname", "닉네임")
        ),
        template(
            NotificationType.MEMBER_AUTH_LOGIN,
            "새로운 로그인 감지",
            "{{occurredAt}}에 로그인되었습니다. 본인이 아니라면 비밀번호를 변경해주세요.",
            Map.of("occurredAt", "로그인 일시")
        ),
        template(
            NotificationType.MEMBER_PASSWORD_CHANGED,
            "비밀번호가 변경되었습니다.",
            "{{occurredAt}}에 비밀번호가 변경되었습니다. 본인이 아니라면 보안센터에 문의해주세요.",
            Map.of("occurredAt", "변경 일시")
        ),
        template(
            NotificationType.MEMBER_DELETE_ACCOUNT,
            "회원 탈퇴가 완료되었습니다.",
            "{{occurredAt}}에 회원 탈퇴 처리가 완료되었습니다.",
            Map.of("occurredAt", "탈퇴 일시")
        ),
        template(
            NotificationType.CHAT_PUBLISHED,
            "{{productName}}님의 새 메시지",
            "{{productName}}님의 채팅: {{message}} ({{sendedAt}})",
            Map.of(
                "productName", "아티스트 또는 상품명",
                "message", "메시지 내용",
                "sendedAt", "전송 일시"
            )
        ),
        template(
            NotificationType.SUBSCRIBE_RENEWAL,
            "{{productName}} 구독 갱신 예정",
            "{{productName}} 구독이 {{daysLeft}}일 후 갱신됩니다. 갱신일은 {{renewalDate}}이며 "
                + "결제 예정 금액은 {{amount}}원입니다.",
            Map.of(
                "productName", "상품명",
                "daysLeft", "갱신까지 남은 일수",
                "renewalDate", "갱신일",
                "amount", "결제 예정 금액"
            )
        )
    );
  }

  private NotificationTemplate template(
      NotificationType notificationType,
      String titleTemplate,
      String contentsTemplate,
      Map<String, Object> variables
  ) {
    return NotificationTemplate.builder()
        .notificationType(notificationType.name())
        .titleTemplate(titleTemplate)
        .contentsTemplate(contentsTemplate)
        .variables(variables)
        .build();
  }
}
