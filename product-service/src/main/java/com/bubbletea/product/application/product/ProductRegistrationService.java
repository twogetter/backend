package com.bubbletea.product.application.product;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.product.dto.ProductResponseDto;
import com.bubbletea.product.application.event.ProductChatEventService;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ProductChangeReservationRepository;
import com.bubbletea.product.presentation.dto.ProductRegisterRequestDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ProductRegistrationService {

    private static final long OPEN_NOTIFICATION_LEAD_DAYS = 3L;

    private final ProductRepository productRepository;
    private final ProductChangeReservationRepository productChangeReservationRepository;
    private final ProductChatEventService productChatEventService;

    public ProductResponseDto register(ProductRegisterRequestDto request) {
        validateDuplicateArtist(request.artistId());

        Product product = Product.schedule(
            request.artistId(),
            request.artistName(),
            request.groupName(),
            request.description(),
            request.imageUrl(),
            request.price(),
            request.openDate()
        );

        Product savedProduct = productRepository.save(product);

        createOpenSchedule(savedProduct);

        String registeredIdempotencyKey = savedProduct.getId() + ":ARTIST_REGISTERED";
        productChatEventService.productRegistered(
            registeredIdempotencyKey, savedProduct.getArtistId(), savedProduct.getCreatedAt()
        );

        return ProductResponseDto.from(savedProduct);
    }

    private void validateDuplicateArtist(String artistId) {
        if (productRepository.existsByArtistIdAndDeletedFalse(artistId)) {
            throw new AppException(ProductErrorCode.DUPLICATE_ARTIST_PRODUCT);
        }
    }

    private void createOpenSchedule(Product product) {
        LocalDateTime openDate = product.getOpenDate();
        LocalDateTime notifyAt = openDate.minusDays(OPEN_NOTIFICATION_LEAD_DAYS);

        productChangeReservationRepository.saveAll(List.of(
            ProductChangeReservation.ofNotifyOpenSchedule(
                product.getId(), notifyAt, product.getName(), openDate),
            ProductChangeReservation.ofActivate(product.getId(), openDate)
        ));
    }
}
