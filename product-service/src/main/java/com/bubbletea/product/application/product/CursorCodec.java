package com.bubbletea.product.application.product;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.exception.ProductApplicationErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CursorCodec {

    private static final String DELIMITER = ":::";

    public record DecodedCursor(String artistName, String id) {

        public static final DecodedCursor EMPTY =
            new DecodedCursor(null, null);
    }

    public static String encode(String artistName, String id) {
        String raw = artistName + DELIMITER + id;
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static DecodedCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return DecodedCursor.EMPTY;
        }

        try {
            String raw = new String(
                Base64.getUrlDecoder().decode(cursor),
                StandardCharsets.UTF_8
            );
            String[] parts = raw.split(DELIMITER, 2);

            if (parts.length != 2) {
                throw new AppException(ProductApplicationErrorCode.INVALID_CURSOR);
            }

            return new DecodedCursor(parts[0], parts[1]);

        } catch (IllegalArgumentException e) {
            throw new AppException(ProductApplicationErrorCode.INVALID_CURSOR);
        }
    }
}
