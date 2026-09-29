package online.lifeasgame.quest.application.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.inventory.domain.seed.SeedLevel1Item;
import online.lifeasgame.quest.application.blueprint.SeedLevel1QuestBlueprintAdapter;
import online.lifeasgame.quest.domain.seed.SeedLevel1Quest;
import online.lifeasgame.reward.domain.seed.SeedLevel1RewardProfile;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private Flyway flyway;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("migration과 bootstrap으로 설치된 여섯 Quest의 실행 필드가 Java Blueprint와 일치한다")
    void materializesSeedDefinitionsIdempotently() {
        var expected = SeedLevel1Quest.definitions().stream()
                .map(SeedLevel1QuestBlueprintAdapter::toBlueprint)
                .map(blueprint -> new QuestRow(
                        blueprint.code().value(),
                        blueprint.category() == null ? null : blueprint.category().name(),
                        blueprint.semanticCategory().name(), blueprint.progressSource().name(),
                        blueprint.target().type().name(), blueprint.target().value(),
                        blueprint.completionPolicy().name(), blueprint.repeatPolicy().name(),
                        blueprint.definitionVersion(), blueprint.rewardProfileCodeOrNull(),
                        blueprint.roleTemplateCodeOrNull()))
                .toList();
        assertThat(seedQuestRows()).hasSize(6).containsExactlyElementsOf(expected);

        var before = installedContent();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        bootstrapper.run(null);
        assertThat(installedContent()).isEqualTo(before);
    }

    @Test
    @DisplayName("SQL 설치 Quest는 짧은 설명, bootstrap 설치 Quest는 긴 설명을 보존한다")
    void preservesDescriptionProvenance() {
        for (var seed : SeedLevel1Quest.definitions()) {
            String description = switch (seed.questCode()) {
                case Q_RECORD_FIRST_TRACE, Q_RECORD_THREE_TRACES, Q_RECORD_WEEKLY_LOOKBACK ->
                        seed.shortDescriptionKo();
                default -> seed.longDescriptionKo();
            };
            assertThat(jdbcTemplate.queryForMap("""
                    SELECT title_id, description_md, due_at
                    FROM quests WHERE code = ?
                    """, seed.questCode().value()))
                    .as(seed.questCode().value())
                    .containsEntry("title_id", seed.displayNameKo())
                    .containsEntry("description_md", description)
                    .containsEntry("due_at", null);
        }
    }

    @Test
    @DisplayName("두 고정 코드 Item의 설치 정의가 Java catalog와 일치한다")
    void matchesInstalledItemsToJavaDefinitions() throws Exception {
        for (var seed : SeedLevel1Item.definitions()) {
            var row = jdbcTemplate.queryForMap("SELECT * FROM items WHERE code = ?", seed.code().value());
            assertThat(row).as(seed.code().value())
                    .containsEntry("name", seed.name())
                    .containsEntry("category", seed.category().name())
                    .containsEntry("type", seed.type().name())
                    .containsEntry("rarity", seed.rarity().name())
                    .containsEntry("stackable", seed.stackable())
                    .containsEntry("max_stack", seed.maxStack())
                    .containsEntry("max_durability", seed.maxDurability())
                    .containsEntry("description", seed.description())
                    .containsEntry("reward_bound", seed.rewardBound())
                    .containsEntry("equipment_compatibility_kind", null);
            assertThat(objectMapper.readTree((String) row.get("base_attrs")))
                    .as("%s base attributes", seed.code().value())
                    .isEqualTo(objectMapper.valueToTree(seed.baseAttrs().attrs()));
        }
    }

    @Test
    @DisplayName("Quest가 참조하는 활성 Profile의 유효 보상과 Item ID/code 연결이 Java 정의와 일치한다")
    void matchesEffectiveRewardsAndItemReferences() {
        var usedProfiles = jdbcTemplate.queryForList(
                "SELECT DISTINCT reward_profile_code FROM quests ORDER BY reward_profile_code", String.class);
        assertThat(usedProfiles).containsExactlyInAnyOrderElementsOf(
                java.util.stream.Stream.concat(
                        SeedLevel1RewardProfile.definitions().stream().map(seed -> seed.code().value()),
                        java.util.stream.Stream.of("RP_NONE")).toList());
        for (var seed : SeedLevel1RewardProfile.definitions()) {
            assertThat(jdbcTemplate.queryForMap("""
                    SELECT name, status, entitlement_code FROM reward_profiles WHERE code = ?
                    """, seed.code().value()))
                    .as(seed.code().value())
                    .containsEntry("name", seed.name())
                    .containsEntry("status", seed.status().name())
                    .containsEntry("entitlement_code", seed.code().value().equals("RP_ADVENTURE_PREPARATION")
                            ? "ADVENTURE_PREPARATION" : null);

            // Java profiles specify amounts; SQL may instead inherit the definition amount.
            // Type/item mappings are the approved payload contract, absent from the Java profile catalog.
            var expected = seed.lines().stream().map(line -> {
                assertThat(line.amountOverride()).as("Java amount for %s", line.definitionCode()).isNotNull();
                String type = switch (line.definitionCode()) {
                    case EXP_PLAYER -> "EXP";
                    case RD_ADVENTURE_GOLD -> "GOLD";
                    case ITEM_DEFINITION, RD_RECORD_CRYSTAL -> "ITEM";
                };
                String item = switch (line.definitionCode()) {
                    case ITEM_DEFINITION -> "IT_FIRST_STEP_FRAGMENT";
                    case RD_RECORD_CRYSTAL -> "IT_RECORD_CRYSTAL";
                    default -> null;
                };
                return new RewardLineRow(line.definitionCode().value(), line.sortOrder(), type,
                        line.amountOverride(), item, item, true);
            }).toList();
            assertThat(rewardLines(seed.code().value())).as(seed.code().value())
                    .containsExactlyElementsOf(expected);
        }
    }

    @Test
    @Transactional
    @DisplayName("재실행은 기존 편집값과 ID·참조·Outbox를 덮어쓰거나 중복 생성하지 않는다")
    void preservesExistingValuesOnRerun() {
        // Changes stay in this isolated test transaction and roll back after the assertion.
        jdbcTemplate.update("""
                UPDATE quests SET title_id = 'Existing title', description_md = 'Existing description',
                    definition_version = 7, target_value = 9
                WHERE code IN ('Q_RECORD_FIRST_TRACE', 'Q_ADVENTURE_PREPARATION')
                """);
        jdbcTemplate.update("UPDATE items SET name = 'Existing item' WHERE code = 'IT_RECORD_CRYSTAL'");
        jdbcTemplate.update("UPDATE reward_definitions SET amount = 101 WHERE code = 'RD_ADVENTURE_GOLD'");
        jdbcTemplate.update("UPDATE quest_routes SET title = 'Existing route' WHERE code = 'ROUTE_RECORD_START'");
        var before = installedContent();

        assertThat(flyway.migrate().migrationsExecuted).isZero();
        bootstrapper.run(null);
        bootstrapper.run(null);

        assertThat(installedContent()).isEqualTo(before);
    }

    private List<RewardLineRow> rewardLines(String profileCode) {
        return jdbcTemplate.query("""
                SELECT definition.code, line.sort_order, definition.reward_type,
                    COALESCE(line.amount_override, definition.amount) AS effective_amount,
                    definition.item_code, item.code AS linked_item_code, definition.active
                FROM reward_profiles profile
                JOIN reward_profile_lines line ON line.reward_profile_id = profile.id
                JOIN reward_definitions definition ON definition.id = line.reward_definition_id
                LEFT JOIN items item ON item.id = definition.item_id
                WHERE profile.code = ? ORDER BY line.sort_order
                """, (rs, index) -> new RewardLineRow(rs.getString("code"), rs.getInt("sort_order"),
                rs.getString("reward_type"), rs.getLong("effective_amount"), rs.getString("item_code"),
                rs.getString("linked_item_code"), rs.getBoolean("active")), profileCode);
    }

    private Map<String, List<Map<String, Object>>> installedContent() {
        var rows = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String table : List.of("quests", "items", "reward_profiles", "reward_definitions",
                "reward_profile_lines", "quest_routes", "quest_route_steps", "quest_route_step_quests",
                "outbox_events")) {
            String order = table.equals("quest_route_step_quests") ? "step_id, quest_id" : "id";
            rows.put(table, jdbcTemplate.queryForList("SELECT * FROM " + table + " ORDER BY " + order));
        }
        return rows;
    }

    private record RewardLineRow(String definitionCode, int sortOrder, String type, long effectiveAmount,
                                 String itemCode, String linkedItemCode, boolean active) {
    }

    private List<QuestRow> seedQuestRows() {
        return jdbcTemplate.query("""
                SELECT
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
                    'Q_RECOVERY_REST_TEN',
                    'Q_ADVENTURE_PREPARATION'
                )
                ORDER BY FIELD(
                    code,
                    'Q_RECORD_FIRST_TRACE',
                    'Q_RECORD_THREE_TRACES',
                    'Q_RECORD_WEEKLY_LOOKBACK',
                    'Q_GROWTH_ONE_FOCUS',
                    'Q_RECOVERY_REST_TEN',
                    'Q_ADVENTURE_PREPARATION'
                )
                """, (resultSet, rowNumber) -> new QuestRow(
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

    private record QuestRow(
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
