package online.lifeasgame.quest.application.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "app.quest.definition-bootstrap.enabled=true",
        "app.outbox.enabled=false"
})
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Quest Definition Bootstrapper MySQL integration")
class QuestDefinitionBootstrapperIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quest_bootstrap")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

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

    @Autowired
    private QuestDefinitionBootstrapper bootstrapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V1~V14와 Reward Seed 위에서 신규 5개를 만들고 재실행해도 중복하지 않는다")
    void materializesSeedDefinitionsIdempotently() {
        List<QuestRow> before = seedQuestRows();

        assertThat(before).hasSize(5);
        assertThat(before).containsExactly(
                new QuestRow(
                        before.get(0).id(),
                        "Q_RECORD_FIRST_TRACE",
                        null,
                        "RECORD",
                        "RECORD_CREATED",
                        "COUNT",
                        1,
                        "AUTO",
                        "ONCE",
                        1,
                        "RP_EXP_TINY_10",
                        null
                ),
                new QuestRow(
                        before.get(1).id(),
                        "Q_RECORD_THREE_TRACES",
                        null,
                        "RECORD",
                        "RECORD_CREATED",
                        "COUNT",
                        3,
                        "AUTO",
                        "ONCE",
                        1,
                        "RP_EXP_AND_ITEM_FIRST_STEP_20",
                        null
                ),
                new QuestRow(
                        before.get(2).id(),
                        "Q_RECORD_WEEKLY_LOOKBACK",
                        null,
                        "RECORD",
                        "RECORD_CREATED",
                        "COUNT",
                        1,
                        "AUTO",
                        "WEEKLY",
                        1,
                        "RP_NONE",
                        null
                ),
                new QuestRow(
                        before.get(3).id(),
                        "Q_GROWTH_ONE_FOCUS",
                        null,
                        "GROWTH",
                        "MANUAL_CHECK",
                        "MINUTES",
                        25,
                        "USER_CONFIRM",
                        "DAILY",
                        1,
                        "RP_NONE",
                        null
                ),
                new QuestRow(
                        before.get(4).id(),
                        "Q_RECOVERY_REST_TEN",
                        null,
                        "RECOVERY",
                        "MANUAL_CHECK",
                        "MINUTES",
                        10,
                        "USER_CONFIRM",
                        "DAILY",
                        1,
                        "RP_NONE",
                        null
                )
        );

        bootstrapper.run(null);

        assertThat(seedQuestRows()).isEqualTo(before);
        assertThat(seedQuestCount()).isEqualTo(5);
    }

    private List<QuestRow> seedQuestRows() {
        return jdbcTemplate.query("""
                SELECT
                    id,
                    code,
                    category,
                    semantic_category,
                    progress_source,
                    target_type,
                    target_value,
                    completion_policy,
                    repeat_rule,
                    definition_version,
                    reward_profile_code,
                    role_template_code
                FROM quests
                WHERE code IN (
                    'Q_RECORD_FIRST_TRACE',
                    'Q_RECORD_THREE_TRACES',
                    'Q_RECORD_WEEKLY_LOOKBACK',
                    'Q_GROWTH_ONE_FOCUS',
                    'Q_RECOVERY_REST_TEN'
                )
                ORDER BY FIELD(
                    code,
                    'Q_RECORD_FIRST_TRACE',
                    'Q_RECORD_THREE_TRACES',
                    'Q_RECORD_WEEKLY_LOOKBACK',
                    'Q_GROWTH_ONE_FOCUS',
                    'Q_RECOVERY_REST_TEN'
                )
                """, (resultSet, rowNumber) -> new QuestRow(
                resultSet.getLong("id"),
                resultSet.getString("code"),
                resultSet.getString("category"),
                resultSet.getString("semantic_category"),
                resultSet.getString("progress_source"),
                resultSet.getString("target_type"),
                resultSet.getInt("target_value"),
                resultSet.getString("completion_policy"),
                resultSet.getString("repeat_rule"),
                resultSet.getInt("definition_version"),
                resultSet.getString("reward_profile_code"),
                resultSet.getString("role_template_code")
        ));
    }

    private int seedQuestCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM quests
                WHERE code IN (
                    'Q_RECORD_FIRST_TRACE',
                    'Q_RECORD_THREE_TRACES',
                    'Q_RECORD_WEEKLY_LOOKBACK',
                    'Q_GROWTH_ONE_FOCUS',
                    'Q_RECOVERY_REST_TEN'
                )
                """, Integer.class);
    }

    private record QuestRow(
            long id,
            String code,
            String category,
            String semanticCategory,
            String progressSource,
            String targetType,
            int targetValue,
            String completionPolicy,
            String repeatRule,
            int definitionVersion,
            String rewardProfileCode,
            String roleTemplateCode
    ) {
    }
}
