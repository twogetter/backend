package com.bubbletea.notification.service;

import com.bubbletea.notification.exception.NotificationErrorCode;
import com.bubbletea.notification.exception.NotificationException;
import com.bubbletea.notification.service.dto.NotificationResponseDto;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationSseService {

  private static final long SSE_TIMEOUT_MILLISECONDS = 60 * 60 * 1000L;
  private static final long HEARTBEAT_INTERVAL_MILLISECONDS = 25_000L;
  private static final long RECONNECT_INTERVAL_MILLISECONDS = 3_000L;

  private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public SseEmitter connect(Long receiverId) {
    validateReceiverId(receiverId);

    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLISECONDS);
    emitters.computeIfAbsent(receiverId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

    emitter.onCompletion(() -> removeEmitter(receiverId, emitter));
    emitter.onTimeout(() -> removeEmitter(receiverId, emitter));
    emitter.onError(ignored -> removeEmitter(receiverId, emitter));

    try {
      emitter.send(SseEmitter.event()
          .name("connect")
          .reconnectTime(RECONNECT_INTERVAL_MILLISECONDS)
          .data(Map.of("receiverId", receiverId)));
    } catch (IOException exception) {
      handleSendFailure(receiverId, emitter, exception);
    }

    return emitter;
  }

  public void sendNotification(Long receiverId, NotificationResponseDto notification) {
    Set<SseEmitter> receiverEmitters = emitters.get(receiverId);
    if (receiverEmitters == null) {
      return;
    }

    for (SseEmitter emitter : receiverEmitters) {
      try {
        emitter.send(SseEmitter.event()
            .id(notification.id().toString())
            .name("notification")
            .data(notification));
      } catch (IOException | IllegalStateException exception) {
        handleSendFailure(receiverId, emitter, exception);
      }
    }
  }

  @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MILLISECONDS)
  public void sendHeartbeat() {
    emitters.forEach((receiverId, receiverEmitters) -> {
      for (SseEmitter emitter : receiverEmitters) {
        try {
          emitter.send(SseEmitter.event().comment("heartbeat"));
        } catch (IOException | IllegalStateException exception) {
          handleSendFailure(receiverId, emitter, exception);
        }
      }
    });
  }

  private void handleSendFailure(Long receiverId, SseEmitter emitter, Exception exception) {
    removeEmitter(receiverId, emitter);
    try {
      emitter.completeWithError(exception);
    } catch (IllegalStateException ignored) {
      // 이미 완료된 연결은 emitter 목록에서 제거하는 것으로 충분합니다.
    }
  }

  private void removeEmitter(Long receiverId, SseEmitter emitter) {
    Set<SseEmitter> receiverEmitters = emitters.get(receiverId);
    if (receiverEmitters == null) {
      return;
    }

    receiverEmitters.remove(emitter);
    if (receiverEmitters.isEmpty()) {
      emitters.remove(receiverId, receiverEmitters);
    }
  }

  private void validateReceiverId(Long receiverId) {
    if (receiverId == null || receiverId < 1) {
      throw new NotificationException(NotificationErrorCode.INVALID_RECEIVER_ID);
    }
  }
}
