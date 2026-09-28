package com.threatpulse;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need real PostgreSQL (with pgvector) and Kafka.
 * <p>
 * The containers follow the singleton pattern: they are started once for the whole test run
 * and shared by every subclass. Spring caches the application context between test classes,
 * and that cached context keeps the connection details of the first containers it saw.
 * If each class started and stopped its own containers, every class after the first
 * would talk to a dead database. Testcontainers removes the containers when the JVM exits.
 * <p>
 * Because the database is shared, tests must not depend on it being empty: clean the tables
 * you use in @BeforeEach and use unique values for unique columns.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("threatpulse_test")
                    .withUsername("test")
                    .withPassword("test");

    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    static {
        postgres.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
