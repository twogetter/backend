package com.bubbletea.product.application.product;


import com.bubbletea.common.exception.AppException;
import com.bubbletea.product.application.exception.ProductApplicationErrorCode;
import com.bubbletea.product.application.product.CursorCodec.DecodedCursor;
import com.bubbletea.product.application.product.dto.ProductDetailResponseDto;
import com.bubbletea.product.application.product.dto.ProductListItemDto;
import com.bubbletea.product.application.product.dto.ProductListResponseDto;
import com.bubbletea.product.domain.exception.ProductErrorCode;
import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductListCondition;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.product.ProductSearchCondition;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductDetailResponseDto getProductDetail(String productId) {
        Product product = productRepository.findActiveProductById(productId)
            .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return ProductDetailResponseDto.from(product);
    }

    public ProductListResponseDto getProductList(String groupName, String cursor, int size) {
        DecodedCursor decodedCursor = CursorCodec.decode(cursor);

        ProductListCondition condition = new ProductListCondition(
            groupName, decodedCursor.artistName(), decodedCursor.id(), size
        );

        List<Product> products = productRepository.findActiveProducts(condition);

        boolean hasNext = products.size() > size;
        List<Product> pageContent = hasNext ? products.subList(0, size) : products;

        String nextCursor = null;
        if (hasNext && !pageContent.isEmpty()) {
            Product last = pageContent.getLast();
            nextCursor = CursorCodec.encode(last.getArtistName(), last.getId());
        }

        List<ProductListItemDto> items = pageContent.stream()
            .map(ProductListItemDto::from)
            .toList();

        return new ProductListResponseDto(items, nextCursor, hasNext);
    }

    public List<String> getFilterableGroupNames() {
        return productRepository.findDistinctActiveGroupNames();
    }

    public ProductListResponseDto searchProducts(String keyword, String cursor, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new AppException(ProductApplicationErrorCode.INVALID_SEARCH_KEYWORD);
        }

        DecodedCursor decodedCursor = CursorCodec.decode(cursor);

        ProductSearchCondition condition = new ProductSearchCondition(
            keyword, decodedCursor.artistName(), decodedCursor.id(), size
        );

        List<Product> products = productRepository.searchActiveProducts(condition);

        boolean hasNext = products.size() > size;
        List<Product> pageContent = hasNext ? products.subList(0, size) : products;

        String nextCursor = null;
        if (hasNext && !pageContent.isEmpty()) {
            Product last = pageContent.getLast();
            nextCursor = CursorCodec.encode(last.getArtistName(), last.getId());
        }

        List<ProductListItemDto> items = pageContent.stream()
            .map(ProductListItemDto::from)
            .toList();

        return new ProductListResponseDto(items, nextCursor, hasNext);
    }

}
