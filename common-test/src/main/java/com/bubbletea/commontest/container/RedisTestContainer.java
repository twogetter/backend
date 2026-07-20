package com.bubbletea.commontest.container;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public interface RedisTestContainer {

    GenericContainer<?> REDIS_CONTAINER = createAndStartContainer();

    private static GenericContainer<?> createAndStartContainer() {
        GenericContainer<?> container = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port",
            () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    @BeforeEach
    default void cleanUpRedis() {
        String uri = "redis://" + REDIS_CONTAINER.getHost() + ":" + REDIS_CONTAINER.getMappedPort(6379);
        try (RedisClient client = RedisClient.create(uri);
            StatefulRedisConnection<String, String> conn = client.connect()) {
            conn.sync().flushall();
        }
    }

}
