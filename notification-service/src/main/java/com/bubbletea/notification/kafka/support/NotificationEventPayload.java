package com.bubbletea.notification.kafka.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NotificationEventPayload {

  private NotificationEventPayload() {
  }

  public static Map<String, Object> of(Object... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException("key-value 매칭 오류.");
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    for (int index = 0; index < keyValues.length; index += 2) {
      Object key = keyValues[index];
      Object value = keyValues[index + 1];
      if (value != null) {
        payload.put(String.valueOf(key), value);
      }
    }
    return payload;
  }
}
