package com.bubbletea.commontest.container;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;


public interface MongoTestContainer {

    MongoDBContainer MONGO_CONTAINER = createAndStartContainer();

    private static MongoDBContainer createAndStartContainer() {
        MongoDBContainer container = new MongoDBContainer("mongo:8")
            .withReuse(true);
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", MONGO_CONTAINER::getReplicaSetUrl);
    }

    @BeforeEach
    default void cleanUpMongo() {
        try (MongoClient client = MongoClients.create(MONGO_CONTAINER.getReplicaSetUrl())) {
            MongoDatabase db = client.getDatabase(
                com.mongodb.ConnectionString.class.cast(
                    new com.mongodb.ConnectionString(MONGO_CONTAINER.getReplicaSetUrl())
                ).getDatabase());
            db.listCollectionNames().forEach(name -> db.getCollection(name).drop());
        }
    }
}
