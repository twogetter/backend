package com.bubbletea.product.infrastructure.mongo.product;

import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductListCondition;
import com.bubbletea.product.domain.product.ProductRepository;
import com.bubbletea.product.domain.product.ProductStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProductRepositoryImpl implements ProductRepository {

    private final ProductMongoRepository productMongoRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Product save(Product product) {
        return productMongoRepository.save(product);
    }

    @Override
    public Optional<Product> findById(String productId) {
        return productMongoRepository.findById(productId);
    }

    @Override
    public boolean existsByArtistIdAndDeletedFalse(String artistId) {
        return productMongoRepository.existsByArtistIdAndDeletedFalse(artistId);
    }

    @Override
    public Optional<Product> findActiveProductById(String productId) {
        Criteria criteria = Criteria.where("_id").is(productId)
            .and("status").is(ProductStatus.ACTIVE)
            .and("deleted").is(false);
        return Optional.ofNullable(mongoTemplate.findOne(Query.query(criteria), Product.class));
    }

    @Override
    public List<Product> findActiveProducts(ProductListCondition condition) {
        Criteria criteria = Criteria.where("status").is(ProductStatus.ACTIVE)
            .and("deleted").is(false);

        List<Criteria> andConditions = new java.util.ArrayList<>();
        andConditions.add(criteria);

        if (condition.hasGroupNameFilter()) {
            Criteria groupOrArtist = new Criteria().orOperator(
                Criteria.where("groupName").is(condition.groupNameFilter()),
                new Criteria().andOperator(
                    Criteria.where("groupName").isNull(),
                    Criteria.where("artistName").is(condition.groupNameFilter())
                )
            );
            andConditions.add(groupOrArtist);
        }

        if (condition.hasCursor()) {
            Criteria cursorCriteria = new Criteria().orOperator(
                Criteria.where("artistName").gt(condition.cursorArtistName()),
                new Criteria().andOperator(
                    Criteria.where("artistName").is(condition.cursorArtistName()),
                    Criteria.where("_id").gt(condition.cursorId())
                )
            );
            andConditions.add(cursorCriteria);
        }

        Criteria finalCriteria = new Criteria()
            .andOperator(andConditions.toArray(new Criteria[0]));

        Query query = Query.query(finalCriteria)
            .with(Sort.by(Sort.Order.asc("artistName"), Sort.Order.asc("_id")))
            .limit(condition.size() + 1);

        return mongoTemplate.find(query, Product.class);
    }

    @Override
    public List<String> findDistinctActiveGroupNames() {
        Criteria criteria = Criteria.where("status").is(ProductStatus.ACTIVE)
            .and("deleted").is(false)
            .and("groupName").ne(null);
        Query query = Query.query(criteria);
        return mongoTemplate.findDistinct(query, "groupName", Product.class, String.class);
    }

}
