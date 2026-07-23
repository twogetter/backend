package com.bubbletea.product.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.exception.ProductApplicationErrorCode;
import com.bubbletea.product.application.product.CursorCodec.DecodedCursor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    @Nested
    @DisplayName("encode()")
    class Encode {

        @Test
        @DisplayName("artistName과 id를 인코딩하면 Base64 기반 커서 문자열을 반환한다.")
        void encodeArtistNameAndId_returnsBase64Cursor() {
            // given
            String artistName = "아티스트1";
            String id = "product-1";

            // when
            String cursor = CursorCodec.encode(artistName, id);

            // then
            assertThat(cursor).isNotNull();
            assertThat(cursor).contains(":::");
        }

        @Test
        @DisplayName("특수문자가 포함된 값도 인코딩할 수 있다.")
        void encodeWithSpecialCharacters_succeeds() {
            // given
            String artistName = "아티스트!@#$%";
            String id = "product/1+2=3";

            // when & then
            assertThat(CursorCodec.encode(artistName, id)).isNotNull();
        }
    }

    @Nested
    @DisplayName("decode()")
    class Decode {

        @Test
        @DisplayName("encode 후 decode하면 원래 값을 복원한다.")
        void encodeAndDecode_restoresOriginalValues() {
            // given
            String artistName = "아티스트1";
            String id = "product-1";

            // when
            String encoded = CursorCodec.encode(artistName, id);
            DecodedCursor decoded = CursorCodec.decode(encoded);

            // then
            assertThat(decoded.artistName()).isEqualTo(artistName);
            assertThat(decoded.id()).isEqualTo(id);
        }

        @Test
        @DisplayName("cursor가 null이면 EMPTY DecodedCursor를 반환한다.")
        void nullCursor_returnsEmptyDecodedCursor() {
            // when
            DecodedCursor result = CursorCodec.decode(null);

            // then
            assertThat(result).isEqualTo(DecodedCursor.EMPTY);
            assertThat(result.artistName()).isNull();
            assertThat(result.id()).isNull();
        }

        @Test
        @DisplayName("cursor가 빈 문자열이면 EMPTY DecodedCursor를 반환한다.")
        void blankCursor_returnsEmptyDecodedCursor() {
            // when
            DecodedCursor result = CursorCodec.decode("   ");

            // then
            assertThat(result).isEqualTo(DecodedCursor.EMPTY);
        }

        @Test
        @DisplayName("구분자가 없는 잘못된 cursor이면 INVALID_CURSOR AppException을 던진다.")
        void cursorWithoutDelimiter_throwsInvalidCursorException() {
            // given
            String invalidCursor = "invalidcursorwithoutdelimiter";

            // when & then
            assertThatThrownBy(() -> CursorCodec.decode(invalidCursor))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                    .isEqualTo(ProductApplicationErrorCode.INVALID_CURSOR)
                );
        }

        @Test
        @DisplayName("디코딩 불가한 cursor이면 INVALID_CURSOR AppException을 던진다.")
        void undecodeableCursor_throwsInvalidCursorException() {
            // given
            String invalidBase64Cursor = "!@#$:::!@#$";

            // when & then
            assertThatThrownBy(() -> CursorCodec.decode(invalidBase64Cursor))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                    .isEqualTo(ProductApplicationErrorCode.INVALID_CURSOR)
                );
        }

    }
}