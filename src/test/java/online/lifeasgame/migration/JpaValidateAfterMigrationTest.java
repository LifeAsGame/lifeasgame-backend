package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import online.lifeasgame.reward.application.RewardProfileReader;
import online.lifeasgame.reward.application.RewardProfileQueryService;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("V2 migration 이후 JPA schema validation")
class JpaValidateAfterMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_jpa_validate")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Flyway flyway;

    @Autowired
    private RewardProfileReader rewardProfileReader;

    @Autowired
    private RewardProfileQueryService rewardProfileQueryService;

    @Nested
    @DisplayName("V1과 V2가 적용된 schema로 ApplicationContext를 기동할 때")
    class LoadApplicationContext {

        @Test
        @DisplayName("ddl-auto validate 상태로 정상 기동한다")
        void loadsWithJpaValidation() {
            assertThat(applicationContext).isNotNull();
            assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
            assertThat(applicationContext.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
            assertThat(applicationContext.getEnvironment()
                    .getProperty("spring.flyway.baseline-on-migrate", Boolean.class))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("V2 Reward seed profile을 조회할 때")
    class LoadRewardSeedProfile {

        @Test
        @DisplayName("EntityGraph로 line과 RewardDefinition을 함께 조회한다")
        void loadsLinesWithDefinitions() {
            var profile = rewardProfileReader.getActiveByCodeOrThrow("RP_EXP_10");

            assertThat(profile.getLines()).hasSize(1);
            assertThat(profile.getLines().getFirst().getRewardDefinition().getCode())
                    .isEqualTo("RD_EXP_10");
        }

        @Test
        @DisplayName("DTO projection으로 활성 profile 요약을 조회한다")
        void loadsActiveProfileSummariesWithProjection() {
            assertThat(rewardProfileQueryService.listActiveProfiles())
                    .extracting(summary -> summary.code())
                    .containsExactly("RP_EXP_10", "RP_EXP_30");
        }
    }
}
