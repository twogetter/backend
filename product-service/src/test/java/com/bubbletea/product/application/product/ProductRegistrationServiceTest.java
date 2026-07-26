package com.bubbletea.product.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.event.ProductChatEventService;
import com.bubbletea.product.application.product.dto.ProductResponseDto;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductFixture;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import com.bubbletea.product.presentation.dto.ProductRegisterRequestDto;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductRegistrationServiceTest {

    @InjectMocks
    private ProductRegistrationService productRegistrationService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductChangeReservationRepository productChangeReservationRepository;

    @Mock
    private ProductChatEventService productChatEventService;

    private static final LocalDateTime VALID_OPEN_DATE =
        LocalDateTime.now().plusDays(30);

    private ProductRegisterRequestDto createFullRequest(
        Long artistId, String artistName, String groupName, LocalDateTime openDate
    ) {
        ProductRegisterRequestDto request = mock(ProductRegisterRequestDto.class);
        given(request.artistId()).willReturn(artistId);
        given(request.artistName()).willReturn(artistName);
        given(request.groupName()).willReturn(groupName);
        given(request.description()).willReturn("상품 설명");
        given(request.imageUrl()).willReturn("https://image.example.com/product.jpg");
        given(request.price()).willReturn(10000L);
        given(request.openDate()).willReturn(openDate);
        return request;
    }

    private ProductRegisterRequestDto createRequestWithArtistIdOnly(Long artistId) {
        ProductRegisterRequestDto request = mock(ProductRegisterRequestDto.class);
        given(request.artistId()).willReturn(artistId);
        return request;
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("정상 요청 시 상품을 저장하고 응답 DTO를 반환한다.")
        void validRequest_savesProductAndReturnsDto() {
            // given
            Long artistId = 1L;
            Product savedProduct = ProductFixture.createMock(artistId, VALID_OPEN_DATE);
            ProductRegisterRequestDto request = createFullRequest(
                artistId, "아티스트1", "아이돌", VALID_OPEN_DATE
            );

            given(productRepository.existsByArtistIdAndDeletedFalse(artistId))
                .willReturn(false);
            given(productRepository.save(any(Product.class)))
                .willReturn(savedProduct);

            // when
            ProductResponseDto result = productRegistrationService.register(request);

            // then
            assertThat(result).isNotNull();
            then(productRepository).should().save(any(Product.class));
        }

        @Test
        @DisplayName("정상 요청 시 오픈 알림 예약과 활성화 예약 2건을 저장한다.")
        void validRequest_savesTwoOpenScheduleReservations() {
            // given
            Long artistId = 1L;
            LocalDateTime openDate = LocalDateTime.now().plusDays(30);
            LocalDateTime expectedNotifyAt = openDate.minusDays(3);

            Product savedProduct = ProductFixture.createMock(artistId, openDate);
            ProductRegisterRequestDto request = createFullRequest(
                artistId, "아티스트1", "아이돌", openDate
            );

            given(productRepository.existsByArtistIdAndDeletedFalse(artistId))
                .willReturn(false);
            given(productRepository.save(any(Product.class)))
                .willReturn(savedProduct);

            // when
            productRegistrationService.register(request);

            // then
            then(productChangeReservationRepository).should().saveAll(
                argThat(reservations -> {

                    boolean hasNotifyOpen = reservations.stream().anyMatch(r ->
                        r.getCommandType() == ReservationCommandType.NOTIFY_OPEN_SCHEDULE
                    );
                    boolean hasActivate = reservations.stream().anyMatch(r ->
                        r.getCommandType() == ReservationCommandType.ACTIVATE
                    );
                    boolean notifyScheduledCorrectly = reservations.stream()
                        .filter(r ->
                            r.getCommandType() == ReservationCommandType.NOTIFY_OPEN_SCHEDULE)
                        .allMatch(r -> r.getScheduledAt().equals(expectedNotifyAt));
                    boolean activateScheduledCorrectly = reservations.stream()
                        .filter(r -> r.getCommandType() == ReservationCommandType.ACTIVATE)
                        .allMatch(r -> r.getScheduledAt().equals(openDate));

                    return reservations.size() == 2
                        && hasNotifyOpen
                        && hasActivate
                        && notifyScheduledCorrectly
                        && activateScheduledCorrectly;
                })
            );
        }

        @Test
        @DisplayName("정상 요청 시 채팅 이벤트를 발행한다.")
        void validRequest_publishesChatEvent() {
            // given
            Long artistId = 1L;
            Product savedProduct = ProductFixture.createMock(artistId, VALID_OPEN_DATE);
            ProductRegisterRequestDto request = createFullRequest(
                artistId, "아티스트1", "아이돌", VALID_OPEN_DATE
            );

            given(productRepository.existsByArtistIdAndDeletedFalse(artistId))
                .willReturn(false);
            given(productRepository.save(any(Product.class)))
                .willReturn(savedProduct);

            // when
            productRegistrationService.register(request);

            // then
            then(productChatEventService).should().productRegistered(
                anyString(),
                eq(savedProduct.getArtistId()),
                eq(ProductFixture.DEFAULT_CREATED_AT)
            );
        }

        @Test
        @DisplayName("채팅 이벤트 idempotencyKey는 savedProduct id + ':ARTIST_REGISTERED' 형식이다.")
        void validRequest_idempotencyKeyFormat() {
            // given
            Long artistId = 1L;
            Product savedProduct = ProductFixture.createMock(artistId, VALID_OPEN_DATE);
            ProductRegisterRequestDto request = createFullRequest(
                artistId, "아티스트1", "아이돌", VALID_OPEN_DATE
            );

            given(productRepository.existsByArtistIdAndDeletedFalse(artistId))
                .willReturn(false);
            given(productRepository.save(any(Product.class)))
                .willReturn(savedProduct);

            // when
            productRegistrationService.register(request);

            // then
            String expectedKey = savedProduct.getId() + ":ARTIST_REGISTERED";
            then(productChatEventService).should().productRegistered(
                eq(expectedKey),
                anyLong(),
                eq(ProductFixture.DEFAULT_CREATED_AT)
            );
        }

        @Test
        @DisplayName("동일한 artistId로 이미 등록된 상품이 있으면 AppException을 던진다")
        void duplicateArtist_throwsAppException() {
            // given
            Long artistId = 1L;
            // 중복 검증 후 즉시 예외 → artistId만 필요
            ProductRegisterRequestDto request = createRequestWithArtistIdOnly(artistId);

            given(productRepository.existsByArtistIdAndDeletedFalse(artistId))
                .willReturn(true);

            // when & then
            assertThatThrownBy(() -> productRegistrationService.register(request))
                .isInstanceOf(AppException.class);

            then(productRepository).should(never()).save(any());
            then(productChangeReservationRepository).should(never()).saveAll(anyList());
            then(productChatEventService).should(never())
                .productRegistered(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("오픈 알림 예약의 scheduledAt은 openDate 3일 전으로 설정된다")
        void openNotificationReservation_scheduledAtIsThreeDaysBeforeOpenDate() {
            // given
            Long artistId = 1L;
            LocalDateTime openDate = LocalDateTime.now().plusDays(30);
            LocalDateTime expectedNotifyAt = openDate.minusDays(3);

            Product savedProduct = ProductFixture.createMock(artistId, openDate);
            ProductRegisterRequestDto request = createFullRequest(
                artistId, "아티스트1", "아이돌", openDate
            );

            given(productRepository.existsByArtistIdAndDeletedFalse(artistId))
                .willReturn(false);
            given(productRepository.save(any(Product.class)))
                .willReturn(savedProduct);

            // when
            productRegistrationService.register(request);

            // then
            then(productChangeReservationRepository).should().saveAll(
                argThat(reservations -> reservations.stream()
                    .filter(r ->
                        r.getCommandType() == ReservationCommandType.NOTIFY_OPEN_SCHEDULE)
                    .anyMatch(r -> r.getScheduledAt().equals(expectedNotifyAt)))
            );
        }
    }
}