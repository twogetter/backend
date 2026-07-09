package com.bubbletea.common.utility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class KafkaEventIdConverter {

    private static final String DELIMITER = ":";
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private KafkaEventIdConverter() {
    }

//  LocalDateTime에 대응하는 함수
    public static String convert(
            String sourceDomain,
            String eventType,
            Long resourceId,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(createdAt, "생성일을 비워둘 수 없습니다.");
        return convert(sourceDomain, eventType, resourceId, createdAt.toLocalDate());
    }
//  LocalDate에 대응하는 함수
    public static String convert(
            String sourceDomain,
            String eventType,
            Long resourceId,
            LocalDate createdAt
    ) {
        validateToken(sourceDomain, "sourceDomain");
        validateToken(eventType, "eventType");
        Objects.requireNonNull(resourceId, "resourceId를 비워둘 수 없습니다.");
        Objects.requireNonNull(createdAt, "생성일을 비워둘 수 없습니다.");

        return String.join(
                DELIMITER,
                sourceDomain,
                eventType,
                resourceId.toString(),
                createdAt.format(CREATED_AT_FORMATTER)
        );
    }

    private static void validateToken(String token, String name) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(name + "은 비워둘 수 없습니다.");
        }

        if (token.contains(DELIMITER)) {
            throw new IllegalArgumentException(name + "에 '" + DELIMITER + "'가 포함될 수 없습니다.");
        }
    }
}
