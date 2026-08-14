package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("Flyway migration")
class FlywayMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_migration")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Nested
    @DisplayName("빈 MySQL 8 데이터베이스에 migration을 적용할 때")
    class MigrateCleanDatabase {

        @Test
        @DisplayName("V1부터 V24까지 적용되고 equipment compatibility kind를 추가한다")
        void migratesSchemaAndSeedsRewardProfiles() throws Exception {
            Flyway throughV10 = flyway(MigrationVersion.fromVersion("10"));
            MigrateResult legacyResult = throughV10.migrate();
            insertLegacyQuest();
            MigrateResult semanticResult =
                    flyway(MigrationVersion.fromVersion("11")).migrate();
            insertLegacyItemsWithoutCode();
            MigrateResult itemResult =
                    flyway(MigrationVersion.fromVersion("12")).migrate();
            insertLegacyLifeLogs();
            insertLegacyQuestAcceptance();
            Flyway flyway = flyway();

            MigrateResult result = flyway.migrate();

            assertThat(legacyResult.migrationsExecuted).isEqualTo(10);
            assertThat(semanticResult.migrationsExecuted).isEqualTo(1);
            assertThat(itemResult.migrationsExecuted).isEqualTo(1);
            assertThat(result.migrationsExecuted).isEqualTo(12);
            assertThat(appliedVersions())
                    .containsExactly(
                            "1", "2", "3", "4", "5",
                            "6", "7", "8", "9", "10", "11", "12", "13",
                            "14", "15", "16", "17", "18", "19", "20",
                            "21", "22", "23", "24"
                    );
            assertThat(existingTables(
                    "users",
                    "player",
                    "quests",
                    "items",
                    "inventory_entries",
                    "reward_definitions",
                    "reward_profiles",
                    "reward_profile_lines",
                    "reward_settlements",
                    "reward_settlement_lines",
                    "player_growth_changes",
                    "quest_signal_receipts",
                    "outbox_events",
                    "quick_record_request_receipts",
                    "life_log_records",
                    "inventory_reward_deliveries",
                    "roles",
                    "persons",
                    "role_relations",
                    "quest_routes",
                    "quest_route_steps",
                    "quest_route_step_quests",
                    "player_quest_routes",
                    "role_events",
                    "role_event_participants"
            )).containsExactlyInAnyOrder(
                    "users",
                    "player",
                    "quests",
                    "items",
                    "inventory_entries",
                    "reward_definitions",
                    "reward_profiles",
                    "reward_profile_lines",
                    "reward_settlements",
                    "reward_settlement_lines",
                    "player_growth_changes",
                    "quest_signal_receipts",
                    "outbox_events",
                    "quick_record_request_receipts",
                    "life_log_records",
                    "inventory_reward_deliveries",
                    "roles",
                    "persons",
                    "role_relations",
                    "quest_routes",
                    "quest_route_steps",
                    "quest_route_step_quests",
                    "player_quest_routes",
                    "role_events",
                    "role_event_participants"
            );
            assertThat(existingTables(
                    "quick_lifelog_entries",
                    "quick_lifelog_entry_tags",
                    "quick_lifelog_request_receipts"
            )).isEmpty();
            assertThat(seedProfileCodes()).containsExactly(
                    "RP_EXP_10",
                    "RP_EXP_30",
                    "RP_EXP_AND_ITEM_FIRST_STEP_20",
                    "RP_EXP_TINY_10"
            );
            assertThat(noRewardProfile()).isEqualTo(new NoRewardProfile("ACTIVE", 0));
            assertThat(expTenDefinitionCount()).isEqualTo(1);
            assertThat(expTenProfiles()).containsExactly(
                    new ExpTenProfileSeed(
                            "RP_EXP_10",
                            "EXP 10 Profile",
                            "ACTIVE",
                            0,
                            null,
                            "RD_EXP_10",
                            "EXP",
                            10,
                            null,
                            10
                    ),
                    new ExpTenProfileSeed(
                            "RP_EXP_TINY_10",
                            "소량 EXP",
                            "ACTIVE",
                            0,
                            null,
                            "RD_EXP_10",
                            "EXP",
                            10,
                            null,
                            10
                    )
            );
            assertThat(uniqueIndexColumns("reward_settlements", "uq_reward_settlement_source"))
                    .containsExactly("player_id", "source_type", "source_id");
            assertThat(uniqueIndexColumns(
                    "reward_settlement_lines",
                    "uq_reward_settlement_line_sort_order"
            )).containsExactly("reward_settlement_id", "sort_order");
            assertThat(uniqueIndexColumns(
                    "player_growth_changes",
                    "uq_player_growth_change_reward_line"
            )).containsExactly("reward_line_id");
            assertThat(uniqueIndexColumns(
                    "quest_signal_receipts",
                    "uq_quest_signal_receipt_identity"
            )).containsExactly("quest_code", "player_id", "correlation_id");
            assertThat(indexColumns(
                    "quest_signal_receipts",
                    "idx_quest_signal_receipt_player"
            )).containsExactly("player_id");
            assertThat(indexColumns(
                    "quest_signal_receipts",
                    "idx_quest_signal_receipt_created_at"
            )).containsExactly("created_at");
            assertThat(uniqueIndexColumns(
                    "quest_acceptances",
                    "uq_repeat"
            )).containsExactly(
                    "player_id",
                    "quest_id",
                    "period_start",
                    "period_end"
            );
            assertThat(indexColumns(
                    "quest_acceptances",
                    "idx_qa_status"
            )).containsExactly("status");
            assertThat(questAcceptanceFactContextColumns()).isEqualTo(
                    new QuestAcceptanceFactContextColumns(
                            "NO",
                            "YES",
                            20
                    )
            );
            assertThat(legacyQuestAcceptedAt()).isEqualTo(
                    LocalDateTime.of(
                            2026,
                            7,
                            29,
                            12,
                            34,
                            56,
                            123_456_000
                    )
            );
            assertThat(uniqueIndexColumns(
                    "outbox_events",
                    "uq_outbox_event_event_id"
            )).containsExactly("event_id");
            assertThat(indexColumns(
                    "outbox_events",
                    "idx_outbox_event_ready"
            )).containsExactly("status", "next_attempt_at", "id");
            assertThat(indexColumns(
                    "outbox_events",
                    "idx_outbox_event_lease"
            )).containsExactly("status", "locked_at");
            assertThat(indexColumns(
                    "quests",
                    "idx_quest_reward_profile_code"
            )).containsExactly("reward_profile_code");
            assertThat(questDefinitionColumns()).isEqualTo(
                    new QuestDefinitionColumns(
                            "NO",
                            "1",
                            "YES",
                            "reward_exp",
                            "reward_stats"
                    )
            );
            assertThat(legacyQuestContract()).isEqualTo(
                    new LegacyQuestContract(
                            1,
                            null,
                            7,
                            2,
                            null,
                            null,
                            "NONE",
                            null,
                            "MAIN"
                    )
            );
            QuestLegacyCategoryColumn categoryColumn =
                    questLegacyCategoryColumn();
            assertThat(categoryColumn.nullable()).isEqualTo("YES");
            assertThat(categoryColumn.columnType()).isEqualTo(
                    "enum('GUILD','MAIN','PARTY','RECOMMENDED','REPEAT')"
            );
            QuestSemanticColumns semanticColumns = questSemanticColumns();
            assertThat(semanticColumns.semanticCategoryNullable())
                    .isEqualTo("YES");
            assertThat(semanticColumns.progressSourceNullable())
                    .isEqualTo("YES");
            assertThat(semanticColumns.roleTemplateCodeNullable())
                    .isEqualTo("YES");
            assertThat(semanticColumns.repeatPolicyColumnCount()).isZero();
            assertThat(semanticColumns.repeatRuleNullable()).isEqualTo("NO");
            assertThat(semanticColumns.repeatRuleColumnType())
                    .contains(
                            "'ONCE'",
                            "'DAILY'",
                            "'WEEKLY'",
                            "'NONE'",
                            "'MONTHLY'"
                    );
            assertThat(rewardProfileForeignKeyCount()).isZero();
            assertThatThrownBy(FlywayMigrationTest.this::violateDefinitionVersionCheck)
                    .isInstanceOfSatisfying(SQLException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(3819)
                    );
            assertThat(uniqueIndexColumns(
                    "quick_record_request_receipts",
                    "uq_quick_record_request_receipt_identity"
            )).containsExactly("player_id", "idempotency_key");
            assertThat(uniqueIndexColumns(
                    "life_log_records",
                    "uq_life_log_record_source"
            )).containsExactly("source_type", "source_id");
            assertThat(indexColumns(
                    "life_log_records",
                    "idx_life_log_record_player_timeline"
            )).containsExactly("player_id", "occurred_at", "id");
            assertThat(lifeLogRecordColumnNames()).containsExactlyInAnyOrder(
                    "id",
                    "player_id",
                    "source_type",
                    "source_id",
                    "source_definition_version",
                    "subtype",
                    "entry_mode",
                    "reflection_scope",
                    "period_key",
                    "primary_role_id",
                    "role_event_id",
                    "occurred_at",
                    "created_at",
                    "updated_at"
            );
            assertThat(lifeLogRecordColumnNames()).doesNotContain(
                    "title",
                    "body",
                    "content",
                    "memo",
                    "tags",
                    "attachment",
                    "location"
            );
            assertThat(lifeLogRecordCheckConstraints()).containsExactlyInAnyOrder(
                    "ck_life_log_record_source_type",
                    "ck_life_log_record_definition_version",
                    "ck_life_log_record_subtype",
                    "ck_life_log_record_entry_mode",
                    "ck_life_log_record_reflection_scope",
                    "ck_life_log_record_reflection_pairing",
                    "ck_life_log_record_non_reflection_metadata",
                    "ck_life_log_record_primary_role",
                    "ck_life_log_record_role_event"
            );
            assertThat(lifeLogRecordCount()).isZero();
            assertLifeLogRecordReflectionCheckContract();
            insertCanonicalHeadersWithSameSourceId();
            assertThat(canonicalLifeLogIds()).hasSize(2)
                    .doesNotHaveDuplicates();
            assertThatThrownBy(
                    FlywayMigrationTest.this
                            ::insertDuplicateCanonicalSource
            ).isInstanceOfSatisfying(SQLException.class, exception ->
                    assertThat(exception.getSQLState()).startsWith("23")
            );
            assertThat(itemCodeColumn()).isEqualTo(new ItemCodeColumn("YES", 80));
            assertThat(uniqueIndexColumns("items", "uq_item_code"))
                    .containsExactly("code");
            assertThat(legacyItemNullCodeCount()).isEqualTo(2);
            FirstStepFragmentSeed itemSeed = firstStepFragmentSeed();
            assertThat(itemSeed.id()).isPositive();
            assertThat(itemSeed).isEqualTo(
                    new FirstStepFragmentSeed(
                            1,
                            "IT_FIRST_STEP_FRAGMENT",
                            itemSeed.id(),
                            "첫걸음의 조각",
                            "QUEST",
                            "ETC",
                            "COMMON",
                            0,
                            true,
                            99,
                            null
                    )
            );
            assertThat(firstStepRewardSeed()).containsExactly(
                    new FirstStepRewardSeed(
                            "RP_EXP_AND_ITEM_FIRST_STEP_20",
                            "EXP 20 + First Step Fragment",
                            "ACTIVE",
                            0,
                            null,
                            "RD_EXP_20",
                            "EXP 20",
                            "EXP",
                            20,
                            null,
                            true,
                            null
                    ),
                    new FirstStepRewardSeed(
                            "RP_EXP_AND_ITEM_FIRST_STEP_20",
                            "EXP 20 + First Step Fragment",
                            "ACTIVE",
                            1,
                            null,
                            "RD_ITEM_FIRST_STEP_FRAGMENT_1",
                            "First Step Fragment x1",
                            "ITEM",
                            1,
                            itemSeed.id(),
                            true,
                            "IT_FIRST_STEP_FRAGMENT"
                    )
            );
            assertThatThrownBy(FlywayMigrationTest.this::insertDuplicateItemCode)
                    .isInstanceOfSatisfying(SQLException.class, exception ->
                            assertThat(exception.getSQLState()).startsWith("23")
                    );
            insertSettlementIdentity();
            assertThatThrownBy(FlywayMigrationTest.this::insertSettlementIdentity)
                    .isInstanceOfSatisfying(SQLException.class, exception ->
                            assertThat(exception.getSQLState()).startsWith("23")
                    );
        }
    }

    private Flyway flyway() {
        return flyway(null);
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private Set<String> appliedVersions() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT version
                     FROM flyway_schema_history
                     WHERE success = TRUE
                     ORDER BY installed_rank
                     """)) {
            Set<String> versions = new LinkedHashSet<>();
            while (resultSet.next()) {
                versions.add(resultSet.getString("version"));
            }
            return versions;
        }
    }

    private Set<String> existingTables(String... tableNames) throws Exception {
        String placeholders = String.join(", ", Collections.nCopies(tableNames.length, "?"));
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (%s)
                """.formatted(placeholders);

        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < tableNames.length; index++) {
                statement.setString(index + 1, tableNames[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<String> tables = new LinkedHashSet<>();
                while (resultSet.next()) {
                    tables.add(resultSet.getString("table_name"));
                }
                return tables;
            }
        }
    }

    private Set<String> seedProfileCodes() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT profile.code
                     FROM reward_profiles profile
                     JOIN reward_profile_lines line ON line.reward_profile_id = profile.id
                     JOIN reward_definitions definition ON definition.id = line.reward_definition_id
                     WHERE profile.status = 'ACTIVE'
                       AND definition.reward_type = 'EXP'
                     ORDER BY profile.code
                     """)) {
            Set<String> codes = new LinkedHashSet<>();
            while (resultSet.next()) {
                codes.add(resultSet.getString("code"));
            }
            return codes;
        }
    }

    private NoRewardProfile noRewardProfile() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT profile.status, COUNT(line.id) AS line_count
                     FROM reward_profiles profile
                     LEFT JOIN reward_profile_lines line ON line.reward_profile_id = profile.id
                     WHERE profile.code = 'RP_NONE'
                     GROUP BY profile.id, profile.status
                     """)) {
            assertThat(resultSet.next()).isTrue();
            NoRewardProfile result = new NoRewardProfile(
                    resultSet.getString("status"),
                    resultSet.getInt("line_count")
            );
            assertThat(resultSet.next()).isFalse();
            return result;
        }
    }

    private int expTenDefinitionCount() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM reward_definitions
                     WHERE code = 'RD_EXP_10'
                     """)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private List<ExpTenProfileSeed> expTenProfiles() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT
                         profile.code AS profile_code,
                         profile.name AS profile_name,
                         profile.status,
                         line.sort_order,
                         line.amount_override,
                         definition.code AS definition_code,
                         definition.reward_type,
                         definition.amount,
                         definition.item_id,
                         COALESCE(line.amount_override, definition.amount)
                             AS effective_amount
                     FROM reward_profiles profile
                     JOIN reward_profile_lines line
                       ON line.reward_profile_id = profile.id
                     JOIN reward_definitions definition
                       ON definition.id = line.reward_definition_id
                     WHERE profile.code IN ('RP_EXP_10', 'RP_EXP_TINY_10')
                     ORDER BY profile.code
                     """)) {
            List<ExpTenProfileSeed> seeds = new ArrayList<>();
            while (resultSet.next()) {
                seeds.add(new ExpTenProfileSeed(
                        resultSet.getString("profile_code"),
                        resultSet.getString("profile_name"),
                        resultSet.getString("status"),
                        resultSet.getInt("sort_order"),
                        resultSet.getObject("amount_override", Long.class),
                        resultSet.getString("definition_code"),
                        resultSet.getString("reward_type"),
                        resultSet.getLong("amount"),
                        resultSet.getObject("item_id", Long.class),
                        resultSet.getLong("effective_amount")
                ));
            }
            return seeds;
        }
    }

    private List<String> uniqueIndexColumns(String tableName, String indexName) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement("""
                     SELECT column_name
                     FROM information_schema.statistics
                     WHERE table_schema = DATABASE()
                       AND table_name = ?
                       AND index_name = ?
                       AND non_unique = 0
                     ORDER BY seq_in_index
                     """)) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> columns = new ArrayList<>();
                while (resultSet.next()) {
                    columns.add(resultSet.getString("column_name"));
                }
                return columns;
            }
        }
    }

    private List<String> indexColumns(
            String tableName,
            String indexName
    ) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement("""
                     SELECT column_name
                     FROM information_schema.statistics
                     WHERE table_schema = DATABASE()
                       AND table_name = ?
                       AND index_name = ?
                     ORDER BY seq_in_index
                     """)) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> columns = new ArrayList<>();
                while (resultSet.next()) {
                    columns.add(resultSet.getString("column_name"));
                }
                return columns;
            }
        }
    }

    private void insertSettlementIdentity() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO reward_settlements (
                        player_id,
                        source_type,
                        source_id,
                        reward_profile_id,
                        reward_profile_code,
                        status,
                        created_at,
                        updated_at
                    ) VALUES (
                        185,
                        'QUEST_COMPLETION',
                        185001,
                        1,
                        'RP_EXP_10',
                        'PENDING',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);
        }
    }

    private void insertLegacyQuest() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO quests (
                        reward_exp,
                        target_value,
                        created_at,
                        due_at,
                        updated_at,
                        code,
                        title_id,
                        reward_stats,
                        category,
                        description_md,
                        repeat_rule,
                        completion_policy,
                        target_type
                    ) VALUES (
                        7,
                        1,
                        CURRENT_TIMESTAMP(6),
                        NULL,
                        CURRENT_TIMESTAMP(6),
                        'quest:test:v9-legacy',
                        'V9 Legacy Quest',
                        JSON_OBJECT('strength', 2),
                        'MAIN',
                        'V9 legacy row',
                        'NONE',
                        'AUTO',
                        'COUNT'
                    )
                    """);
        }
    }

    private void insertLegacyLifeLogs() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO collection_logs (
                        quantity_value,
                        created_at,
                        player_id,
                        updated_at,
                        title_value,
                        category
                    ) VALUES (
                        1,
                        CURRENT_TIMESTAMP(6),
                        213,
                        CURRENT_TIMESTAMP(6),
                        'Legacy collection',
                        'BOOK'
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO exercise_logs (
                        duration_minutes,
                        exercised_on,
                        created_at,
                        player_id,
                        updated_at,
                        category
                    ) VALUES (
                        10,
                        '2026-07-29',
                        CURRENT_TIMESTAMP(6),
                        213,
                        CURRENT_TIMESTAMP(6),
                        'WALKING'
                    )
                    """);
        }
    }

    private void insertLegacyQuestAcceptance() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO quest_acceptances (
                        period_end,
                        period_start,
                        progress_value,
                        created_at,
                        updated_at,
                        player_id,
                        quest_id,
                        status
                    )
                    SELECT
                        '9999-12-31',
                        '1970-01-01',
                        0,
                        '2026-07-29 12:34:56.123456',
                        '2026-07-29 12:34:56.123456',
                        21300,
                        id,
                        'IN_PROGRESS'
                    FROM quests
                    WHERE code = 'quest:test:v9-legacy'
                    """);
        }
    }

    private QuestAcceptanceFactContextColumns
    questAcceptanceFactContextColumns() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT
                         MAX(CASE WHEN column_name = 'accepted_at'
                             THEN is_nullable END) AS accepted_at_nullable,
                         MAX(CASE WHEN column_name = 'period_key'
                             THEN is_nullable END) AS period_key_nullable,
                         MAX(CASE WHEN column_name = 'period_key'
                             THEN character_maximum_length END)
                             AS period_key_length
                     FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND table_name = 'quest_acceptances'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return new QuestAcceptanceFactContextColumns(
                    resultSet.getString("accepted_at_nullable"),
                    resultSet.getString("period_key_nullable"),
                    resultSet.getInt("period_key_length")
            );
        }
    }

    private LocalDateTime legacyQuestAcceptedAt() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT accepted_at, period_key
                     FROM quest_acceptances
                     WHERE player_id = 21300
                     """)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("period_key")).isNull();
            LocalDateTime acceptedAt = resultSet.getTimestamp("accepted_at")
                    .toLocalDateTime();
            assertThat(resultSet.next()).isFalse();
            return acceptedAt;
        }
    }

    private List<String> lifeLogRecordColumnNames() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT column_name
                     FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND table_name = 'life_log_records'
                     ORDER BY ordinal_position
                     """)) {
            List<String> columns = new ArrayList<>();
            while (resultSet.next()) {
                columns.add(resultSet.getString("column_name"));
            }
            return columns;
        }
    }

    private List<String> lifeLogRecordCheckConstraints()
            throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT constraint_name
                     FROM information_schema.table_constraints
                     WHERE table_schema = DATABASE()
                       AND table_name = 'life_log_records'
                       AND constraint_type = 'CHECK'
                     ORDER BY constraint_name
                     """)) {
            List<String> constraints = new ArrayList<>();
            while (resultSet.next()) {
                constraints.add(resultSet.getString("constraint_name"));
            }
            return constraints;
        }
    }

    private int lifeLogRecordCount() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM life_log_records
                     """)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void assertLifeLogRecordReflectionCheckContract()
            throws SQLException {
        insertLifeLogRecord(21301, null, null, null);
        insertLifeLogRecord(21302, "REFLECTION", null, null);
        insertLifeLogRecord(
                21303,
                "REFLECTION",
                "WEEKLY_LOOKBACK",
                "2026-W31"
        );
        insertLifeLogRecord(21304, "ACTIVITY", null, null);
        assertThat(lifeLogRecordCount()).isEqualTo(4);

        assertLifeLogRecordInsertRejected(
                21305,
                "REFLECTION",
                "WEEKLY_LOOKBACK",
                null
        );
        assertLifeLogRecordInsertRejected(
                21306,
                null,
                "WEEKLY_LOOKBACK",
                null
        );
        assertLifeLogRecordInsertRejected(
                21307,
                "ACTIVITY",
                "WEEKLY_LOOKBACK",
                "2026-W31"
        );
        assertLifeLogRecordInsertRejected(
                21308,
                "REFLECTION",
                "WEEKLY_LOOKBACK",
                "invalid"
        );
        assertThat(lifeLogRecordCount()).isEqualTo(4);
    }

    private void assertLifeLogRecordInsertRejected(
            long sourceId,
            String subtype,
            String reflectionScope,
            String periodKey
    ) throws SQLException {
        int countBefore = lifeLogRecordCount();

        assertThatThrownBy(() -> insertLifeLogRecord(
                sourceId,
                subtype,
                reflectionScope,
                periodKey
        )).isInstanceOf(SQLException.class);

        assertThat(lifeLogRecordCount()).isEqualTo(countBefore);
    }

    private void insertLifeLogRecord(
            long sourceId,
            String subtype,
            String reflectionScope,
            String periodKey
    ) throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement("""
                     INSERT INTO life_log_records (
                         player_id,
                         source_type,
                         source_id,
                         source_definition_version,
                         subtype,
                         entry_mode,
                         reflection_scope,
                         period_key,
                         primary_role_id,
                         occurred_at,
                         created_at,
                         updated_at
                     ) VALUES (
                         213,
                         'COLLECTION',
                         ?,
                         1,
                         ?,
                         'FULL',
                         ?,
                         ?,
                         NULL,
                         CURRENT_TIMESTAMP(6),
                         CURRENT_TIMESTAMP(6),
                         CURRENT_TIMESTAMP(6)
                     )
                     """)) {
            statement.setLong(1, sourceId);
            statement.setString(2, subtype);
            statement.setString(3, reflectionScope);
            statement.setString(4, periodKey);
            statement.executeUpdate();
        }
    }

    private void insertCanonicalHeadersWithSameSourceId()
            throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(canonicalHeaderInsert("COLLECTION"));
            statement.executeUpdate(canonicalHeaderInsert("EXERCISE"));
        }
    }

    private List<Long> canonicalLifeLogIds() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT id
                     FROM life_log_records
                     WHERE source_id = 1
                     ORDER BY id
                     """)) {
            List<Long> ids = new ArrayList<>();
            while (resultSet.next()) {
                ids.add(resultSet.getLong("id"));
            }
            return ids;
        }
    }

    private void insertDuplicateCanonicalSource() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(canonicalHeaderInsert("COLLECTION"));
        }
    }

    private String canonicalHeaderInsert(String sourceType) {
        return """
                INSERT INTO life_log_records (
                    player_id,
                    source_type,
                    source_id,
                    source_definition_version,
                    subtype,
                    entry_mode,
                    reflection_scope,
                    period_key,
                    primary_role_id,
                    occurred_at,
                    created_at,
                    updated_at
                ) VALUES (
                    213,
                    '%s',
                    1,
                    1,
                    NULL,
                    'FULL',
                    NULL,
                    NULL,
                    NULL,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """.formatted(sourceType);
    }

    private void insertLegacyItemsWithoutCode() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO items (
                        max_durability,
                        max_stack,
                        stackable,
                        created_at,
                        updated_at,
                        name,
                        base_attrs,
                        category,
                        rarity,
                        type
                    ) VALUES
                        (
                            NULL, 1, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
                            'Legacy Item One', JSON_OBJECT(), 'MISC', 'COMMON', 'ETC'
                        ),
                        (
                            NULL, 1, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
                            'Legacy Item Two', JSON_OBJECT(), 'MISC', 'COMMON', 'ETC'
                        )
                    """);
        }
    }

    private ItemCodeColumn itemCodeColumn() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT is_nullable, character_maximum_length
                     FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND table_name = 'items'
                       AND column_name = 'code'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return new ItemCodeColumn(
                    resultSet.getString("is_nullable"),
                    resultSet.getInt("character_maximum_length")
            );
        }
    }

    private int legacyItemNullCodeCount() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM items
                     WHERE name LIKE 'Legacy Item %'
                       AND code IS NULL
                     """)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private FirstStepFragmentSeed firstStepFragmentSeed() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT
                         COUNT(*) OVER () AS seed_count,
                         code,
                         id,
                         name,
                         category,
                         type,
                         rarity,
                         JSON_LENGTH(base_attrs) AS base_attr_count,
                         stackable,
                         max_stack,
                         max_durability
                     FROM items
                     WHERE code = 'IT_FIRST_STEP_FRAGMENT'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            FirstStepFragmentSeed seed = new FirstStepFragmentSeed(
                    resultSet.getInt("seed_count"),
                    resultSet.getString("code"),
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getString("category"),
                    resultSet.getString("type"),
                    resultSet.getString("rarity"),
                    resultSet.getInt("base_attr_count"),
                    resultSet.getBoolean("stackable"),
                    resultSet.getInt("max_stack"),
                    resultSet.getObject("max_durability", Integer.class)
            );
            assertThat(resultSet.next()).isFalse();
            return seed;
        }
    }

    private List<FirstStepRewardSeed> firstStepRewardSeed() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT
                         profile.code AS profile_code,
                         profile.name AS profile_name,
                         profile.status,
                         line.sort_order,
                         line.amount_override,
                         definition.code AS definition_code,
                         definition.name AS definition_name,
                         definition.reward_type,
                         definition.amount,
                         definition.item_id,
                         definition.active,
                         item.code AS item_code
                     FROM reward_profiles profile
                     JOIN reward_profile_lines line
                       ON line.reward_profile_id = profile.id
                     JOIN reward_definitions definition
                       ON definition.id = line.reward_definition_id
                     LEFT JOIN items item
                       ON item.id = definition.item_id
                     WHERE profile.code = 'RP_EXP_AND_ITEM_FIRST_STEP_20'
                     ORDER BY line.sort_order
                     """)) {
            List<FirstStepRewardSeed> seeds = new ArrayList<>();
            while (resultSet.next()) {
                seeds.add(new FirstStepRewardSeed(
                        resultSet.getString("profile_code"),
                        resultSet.getString("profile_name"),
                        resultSet.getString("status"),
                        resultSet.getInt("sort_order"),
                        resultSet.getObject("amount_override", Long.class),
                        resultSet.getString("definition_code"),
                        resultSet.getString("definition_name"),
                        resultSet.getString("reward_type"),
                        resultSet.getLong("amount"),
                        resultSet.getObject("item_id", Long.class),
                        resultSet.getBoolean("active"),
                        resultSet.getString("item_code")
                ));
            }
            return seeds;
        }
    }

    private void insertDuplicateItemCode() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO items (
                        code,
                        name,
                        category,
                        type,
                        rarity,
                        base_attrs,
                        stackable,
                        max_stack,
                        max_durability,
                        created_at,
                        updated_at
                    ) VALUES (
                        'IT_FIRST_STEP_FRAGMENT',
                        'Duplicate Stable Item',
                        'QUEST',
                        'ETC',
                        'COMMON',
                        JSON_OBJECT(),
                        TRUE,
                        99,
                        NULL,
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);
        }
    }

    private QuestDefinitionColumns questDefinitionColumns() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement("""
                     SELECT
                         MAX(CASE WHEN column_name = 'definition_version'
                             THEN is_nullable END) AS version_nullable,
                         MAX(CASE WHEN column_name = 'definition_version'
                             THEN column_default END) AS version_default,
                         MAX(CASE WHEN column_name = 'reward_profile_code'
                             THEN is_nullable END) AS profile_nullable,
                         MAX(CASE WHEN column_name = 'reward_exp'
                             THEN column_name END) AS reward_exp_column,
                         MAX(CASE WHEN column_name = 'reward_stats'
                             THEN column_name END) AS reward_stats_column
                     FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND table_name = 'quests'
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            return new QuestDefinitionColumns(
                    resultSet.getString("version_nullable"),
                    resultSet.getString("version_default"),
                    resultSet.getString("profile_nullable"),
                    resultSet.getString("reward_exp_column"),
                    resultSet.getString("reward_stats_column")
            );
        }
    }

    private LegacyQuestContract legacyQuestContract() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT
                         definition_version,
                         reward_profile_code,
                         reward_exp,
                         JSON_EXTRACT(reward_stats, '$.strength') AS strength,
                         semantic_category,
                         progress_source,
                         repeat_rule,
                         role_template_code,
                         category
                     FROM quests
                     WHERE code = 'quest:test:v9-legacy'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return new LegacyQuestContract(
                    resultSet.getInt("definition_version"),
                    resultSet.getString("reward_profile_code"),
                    resultSet.getInt("reward_exp"),
                    resultSet.getInt("strength"),
                    resultSet.getString("semantic_category"),
                    resultSet.getString("progress_source"),
                    resultSet.getString("repeat_rule"),
                    resultSet.getString("role_template_code"),
                    resultSet.getString("category")
            );
        }
    }

    private QuestLegacyCategoryColumn questLegacyCategoryColumn()
            throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT is_nullable, column_type
                     FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND table_name = 'quests'
                       AND column_name = 'category'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return new QuestLegacyCategoryColumn(
                    resultSet.getString("is_nullable"),
                    resultSet.getString("column_type")
            );
        }
    }

    private QuestSemanticColumns questSemanticColumns() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT
                         MAX(CASE WHEN column_name = 'semantic_category'
                             THEN is_nullable END) AS semantic_nullable,
                         MAX(CASE WHEN column_name = 'progress_source'
                             THEN is_nullable END) AS progress_nullable,
                         MAX(CASE WHEN column_name = 'role_template_code'
                             THEN is_nullable END) AS role_nullable,
                         SUM(column_name = 'repeat_policy')
                             AS repeat_policy_count,
                         MAX(CASE WHEN column_name = 'repeat_rule'
                             THEN is_nullable END) AS repeat_rule_nullable,
                         MAX(CASE WHEN column_name = 'repeat_rule'
                             THEN column_type END) AS repeat_rule_type
                     FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND table_name = 'quests'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return new QuestSemanticColumns(
                    resultSet.getString("semantic_nullable"),
                    resultSet.getString("progress_nullable"),
                    resultSet.getString("role_nullable"),
                    resultSet.getInt("repeat_policy_count"),
                    resultSet.getString("repeat_rule_nullable"),
                    resultSet.getString("repeat_rule_type")
            );
        }
    }

    private int rewardProfileForeignKeyCount() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM information_schema.key_column_usage
                     WHERE table_schema = DATABASE()
                       AND table_name = 'quests'
                       AND column_name = 'reward_profile_code'
                       AND referenced_table_name IS NOT NULL
                     """)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void violateDefinitionVersionCheck() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE quests
                    SET definition_version = 0
                    WHERE code = 'quest:test:v9-legacy'
                    """);
        }
    }

    private record NoRewardProfile(String status, int lineCount) {
    }

    private record ExpTenProfileSeed(
            String profileCode,
            String profileName,
            String status,
            int sortOrder,
            Long amountOverride,
            String definitionCode,
            String rewardType,
            long amount,
            Long itemId,
            long effectiveAmount
    ) {
    }

    private record QuestDefinitionColumns(
            String versionNullable,
            String versionDefault,
            String profileNullable,
            String rewardExpColumn,
            String rewardStatsColumn
    ) {
    }

    private record LegacyQuestContract(
            int definitionVersion,
            String rewardProfileCode,
            int rewardExp,
            int strength,
            String semanticCategory,
            String progressSource,
            String repeatRule,
            String roleTemplateCode,
            String category
    ) {
    }

    private record QuestLegacyCategoryColumn(
            String nullable,
            String columnType
    ) {
    }

    private record QuestSemanticColumns(
            String semanticCategoryNullable,
            String progressSourceNullable,
            String roleTemplateCodeNullable,
            int repeatPolicyColumnCount,
            String repeatRuleNullable,
            String repeatRuleColumnType
    ) {
    }

    private record QuestAcceptanceFactContextColumns(
            String acceptedAtNullable,
            String periodKeyNullable,
            int periodKeyLength
    ) {
    }

    private record ItemCodeColumn(String nullable, int maximumLength) {
    }

    private record FirstStepFragmentSeed(
            int count,
            String code,
            long id,
            String name,
            String category,
            String type,
            String rarity,
            int baseAttrCount,
            boolean stackable,
            int maxStack,
            Integer maxDurability
    ) {
    }

    private record FirstStepRewardSeed(
            String profileCode,
            String profileName,
            String status,
            int sortOrder,
            Long amountOverride,
            String definitionCode,
            String definitionName,
            String rewardType,
            long amount,
            Long itemId,
            boolean active,
            String itemCode
    ) {
    }
}
