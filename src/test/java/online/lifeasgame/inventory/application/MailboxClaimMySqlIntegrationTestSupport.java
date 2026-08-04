package online.lifeasgame.inventory.application;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
abstract class MailboxClaimMySqlIntegrationTestSupport {

    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            "mysql:8.0.39"
    ).withDatabaseName("lifeasgame_mailbox_claim_228")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
    }
}
