package com.bubbletea.product.application.product;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.exception.ProductApplicationErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CursorCodec {

    private static final String DELIMITER = ":::";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();


    public record DecodedCursor(String artistName, String id) {
        public static final DecodedCursor EMPTY =
            new DecodedCursor(null, null);
    }

    public static String encode(String artistName, String id) {
        return encodeToBase64(artistName) + DELIMITER + encodeToBase64(id);
    }

    public static DecodedCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return DecodedCursor.EMPTY;
        }

        try {
            String[] parts = cursor.split(DELIMITER, 2);

            if (parts.length != 2) {
                throw new AppException(ProductApplicationErrorCode.INVALID_CURSOR);
            }

            return new DecodedCursor(
                decodeFromBase64(parts[0]),
                decodeFromBase64(parts[1])
            );

        } catch (IllegalArgumentException e) {
            throw new AppException(ProductApplicationErrorCode.INVALID_CURSOR);
        }
    }

    private static String encodeToBase64(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeFromBase64(String value) {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }

}
