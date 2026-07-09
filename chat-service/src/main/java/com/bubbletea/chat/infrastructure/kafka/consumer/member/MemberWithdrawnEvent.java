package com.bubbletea.chat.infrastructure.kafka.consumer.member;

import java.time.LocalDateTime;

public record MemberWithdrawnEvent(String eventId, String eventType, Long memberId,
                                   LocalDateTime withdrawnAt) {

}
