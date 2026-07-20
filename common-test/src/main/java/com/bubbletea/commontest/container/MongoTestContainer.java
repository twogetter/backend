package com.bubbletea.commontest.container;

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
}
