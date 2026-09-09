package online.lifeasgame.migration;

import online.lifeasgame.inventory.application.internal.ItemLookupApi;
import online.lifeasgame.quest.application.QuestQueryService;
import online.lifeasgame.quest.application.bootstrap.QuestDefinitionBootstrapper;
import online.lifeasgame.quest.application.query.QuestQuery;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
import online.lifeasgame.reward.application.result.RewardProfileResult;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import online.lifeasgame.reward.application.RewardProfileReader;
import online.lifeasgame.reward.application.RewardProfileQueryService;
import online.lifeasgame.reward.application.RewardSettlementCreateService;
import online.lifeasgame.reward.application.RewardSettlementReader;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("V34 Consumer content reconciliation 이후 runtime validation")
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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RewardProfileReader rewardProfileReader;

    @Autowired
    private RewardProfileQueryService rewardProfileQueryService;

    @Autowired
    private RewardProfileLookupApi rewardProfileLookupApi;

    @Autowired
    private ItemLookupApi itemLookupApi;

    @Autowired
    private RewardSettlementCreateService rewardSettlementCreateService;

    @Autowired
    private RewardSettlementReader rewardSettlementReader;

    @Autowired
    private QuestQueryService questQueryService;

    @Autowired
    private QuestDefinitionBootstrapper questDefinitionBootstrapper;

    @Nested
    @DisplayName("V1부터 V34까지 적용된 schema로 ApplicationContext를 기동할 때")
    class LoadApplicationContext {

        @Test
        @DisplayName("ddl-auto validate 상태로 정상 기동한다")
        void loadsWithJpaValidation() {
            assertThat(applicationContext).isNotNull();
            assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("34");
            assertThat(applicationContext.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
            assertThat(applicationContext.getEnvironment()
                    .getProperty("spring.flyway.baseline-on-migrate", Boolean.class))
                    .isFalse();
        }

        @Test
        @DisplayName("신규 Seed Quest 5개를 nullable category로 Bootstrap하고 재실행은 no-op이다")
        void bootstrapsSeedQuestsIdempotently() throws Exception {
            var codes = java.util.List.of(
                    QuestCode.Q_RECORD_FIRST_TRACE,
                    QuestCode.Q_RECORD_THREE_TRACES,
                    QuestCode.Q_RECORD_WEEKLY_LOOKBACK,
                    QuestCode.Q_GROWTH_ONE_FOCUS,
                    QuestCode.Q_RECOVERY_REST_TEN
            );
            var before = codes.stream()
                    .map(code -> questQueryService.getDefinition(
                            new QuestQuery.Definition(code.value())
                    ))
                    .toList();

            assertThat(before).allSatisfy(definition -> {
                assertThat(definition.category()).isNull();
                assertThat(definition.semanticCategory()).isNotNull();
                assertThat(definition.progressSource()).isNotNull();
                assertThat(definition.roleTemplateCode()).isNull();
            });
            assertThat(questQueryService.getCatalog())
                    .extracting(definition -> definition.code())
                    .containsExactly(
                            "Q_RECORD_FIRST_TRACE",
                            "Q_RECORD_THREE_TRACES",
                            "Q_RECORD_WEEKLY_LOOKBACK",
                            "Q_GROWTH_ONE_FOCUS",
                            "Q_RECOVERY_REST_TEN"
                    );

            questDefinitionBootstrapper.run(null);

            var after = codes.stream()
                    .map(code -> questQueryService.getDefinition(
                            new QuestQuery.Definition(code.value())
                    ))
                    .toList();
            assertThat(after)
                    .extracting(definition -> definition.id())
                    .containsExactlyElementsOf(
                            before.stream()
                                    .map(definition -> definition.id())
                                    .toList()
                    );
        }
    }

    @Nested
    @DisplayName("승인 Reward Profile을 조회할 때")
    class LoadRewardSeedProfile {

        @Test
        @DisplayName("V5 RP_NONE은 ACTIVE 상태이고 Line이 없다")
        void loadsNoRewardProfileWithoutLines() {
            var profile = rewardProfileReader.getActiveByCodeOrThrow("RP_NONE");

            assertThat(profile.isActive()).isTrue();
            assertThat(profile.getLines()).isEmpty();
        }

        @Test
        @DisplayName("DTO projection으로 활성 profile 요약을 조회한다")
        void loadsActiveProfileSummariesWithProjection() {
            assertThat(rewardProfileQueryService.listActiveProfiles())
                    .extracting(RewardProfileResult.Summary::code)
                    .contains(
                            "RP_EXP_AND_ITEM_FIRST_STEP_20",
                            "RP_EXP_TINY_10",
                            "RP_NONE"
                    );
        }
    }

    @Nested
    @DisplayName("공식 TINY Reward Profile을 조회할 때")
    class LoadTinyExpRewardProfile {

        @Test
        @DisplayName("EXP_PLAYER 첫 line의 amount 10을 반환한다")
        void loadsActiveTinyProfile() {
            var reference = rewardProfileLookupApi.getActiveByCode("RP_EXP_TINY_10");
            var detail = rewardProfileQueryService.getProfileView("RP_EXP_TINY_10");

            assertThat(reference.code()).isEqualTo("RP_EXP_TINY_10");
            assertThat(detail.code()).isEqualTo("RP_EXP_TINY_10");
            assertThat(detail.name()).isEqualTo("소량 EXP");
            assertThat(detail.status()).isEqualTo("ACTIVE");
            assertThat(detail.lines()).hasSize(1);

            RewardProfileResult.Line line = detail.lines().getFirst();
            assertThat(line.sortOrder()).isEqualTo(1);
            assertThat(line.amountOverride()).isEqualTo(10L);
            assertThat(line.effectiveAmount()).isEqualTo(10L);
            assertThat(line.rewardDefinition().code()).isEqualTo("EXP_PLAYER");
            assertThat(line.rewardDefinition().rewardType()).isEqualTo("EXP");
            assertThat(line.rewardDefinition().amount()).isEqualTo(10L);
            assertThat(line.rewardDefinition().itemId()).isNull();
        }
    }

    @Nested
    @DisplayName("first-step Reward Profile을 조회할 때")
    class LoadFirstStepRewardProfile {

        @Test
        @DisplayName("EXP_PLAYER 20 다음 ITEM_DEFINITION x1을 반환한다")
        void loadsActiveProfileWithExpAndStableItemLines() {
            var reference = rewardProfileLookupApi.getActiveByCode(
                    "RP_EXP_AND_ITEM_FIRST_STEP_20"
            );
            var detail = rewardProfileQueryService.getProfileView(
                    "RP_EXP_AND_ITEM_FIRST_STEP_20"
            );
            var item = itemLookupApi.getByCode("IT_FIRST_STEP_FRAGMENT");

            assertThat(reference.code()).isEqualTo("RP_EXP_AND_ITEM_FIRST_STEP_20");
            assertThat(detail.status()).isEqualTo("ACTIVE");
            assertThat(detail.lines()).hasSize(2);

            RewardProfileResult.Line expLine = detail.lines().get(0);
            assertThat(expLine.sortOrder()).isEqualTo(1);
            assertThat(expLine.amountOverride()).isEqualTo(20L);
            assertThat(expLine.effectiveAmount()).isEqualTo(20L);
            assertThat(expLine.rewardDefinition().code()).isEqualTo("EXP_PLAYER");
            assertThat(expLine.rewardDefinition().rewardType()).isEqualTo("EXP");
            assertThat(expLine.rewardDefinition().amount()).isEqualTo(10L);
            assertThat(expLine.effectiveAmount())
                    .isNotEqualTo(expLine.rewardDefinition().amount());
            assertThat(expLine.rewardDefinition().itemId()).isNull();

            RewardProfileResult.Line itemLine = detail.lines().get(1);
            assertThat(itemLine.sortOrder()).isEqualTo(2);
            assertThat(itemLine.amountOverride()).isEqualTo(1L);
            assertThat(itemLine.effectiveAmount()).isEqualTo(1L);
            assertThat(itemLine.rewardDefinition().code())
                    .isEqualTo("ITEM_DEFINITION");
            assertThat(itemLine.rewardDefinition().rewardType()).isEqualTo("ITEM");
            assertThat(itemLine.rewardDefinition().amount()).isEqualTo(1L);
            assertThat(itemLine.rewardDefinition().itemId()).isEqualTo(item.id());
        }
    }

    @Nested
    @DisplayName("V34 Consumer content를 조회할 때")
    class LoadConsumerContentReconciliation {

        @Test
        @DisplayName("Route와 세 Step의 exact text, 순서, required Quest mapping을 보존한다")
        void loadsExactRouteContract() {
            assertThat(jdbcTemplate.queryForMap("""
                    SELECT definition_version, title, description
                    FROM quest_routes
                    WHERE code = 'ROUTE_RECORD_START'
                    """))
                    .containsEntry("definition_version", 1)
                    .containsEntry("title", "기록을 시작하는 길")
                    .containsEntry(
                            "description",
                            "하루의 한 장면을 남기고, 여러 흔적을 이어, 다시 돌아보는 가장 작은 기록 여정."
                    );

            assertThat(jdbcTemplate.query("""
                    SELECT
                        step.step_code,
                        step.step_order,
                        step.title,
                        step.description,
                        required_quest.code AS quest_code,
                        link.requirement_type,
                        step.user_advance_required
                    FROM quest_route_steps step
                    JOIN quest_routes route ON route.id = step.route_id
                    LEFT JOIN quest_route_step_quests link ON link.step_id = step.id
                    LEFT JOIN quests required_quest ON required_quest.id = link.quest_id
                    WHERE route.code = 'ROUTE_RECORD_START'
                    ORDER BY step.step_order
                    """, (resultSet, rowNumber) -> new RouteStepRow(
                    resultSet.getString("step_code"),
                    resultSet.getInt("step_order"),
                    resultSet.getString("title"),
                    resultSet.getString("description"),
                    resultSet.getString("quest_code"),
                    resultSet.getString("requirement_type"),
                    resultSet.getBoolean("user_advance_required")
            )))
                    .extracting(
                            RouteStepRow::code,
                            RouteStepRow::order,
                            RouteStepRow::title,
                            RouteStepRow::description,
                            RouteStepRow::questCode,
                            RouteStepRow::requirementType,
                            RouteStepRow::userAdvanceRequired
                    )
                    .containsExactly(
                            tuple(
                                    "RS_RECORD_01_LEAVE_TRACE", 1,
                                    "한 장면 남기기",
                                    "첫 LifeLog를 남겨 여정의 출발점을 만듭니다.",
                                    "Q_RECORD_FIRST_TRACE", "REQUIRED", true
                            ),
                            tuple(
                                    "RS_RECORD_02_CONNECT_TRACES", 2,
                                    "흔적 이어보기",
                                    "서로 다른 기록 세 개를 이어 작은 흐름을 만듭니다.",
                                    "Q_RECORD_THREE_TRACES", "REQUIRED", true
                            ),
                            tuple(
                                    "RS_RECORD_03_LOOK_BACK", 3,
                                    "돌아보고 다음 장 열기",
                                    "쌓인 기록을 돌아보고 한 줄을 남긴 뒤 Route를 마칩니다.",
                                    "Q_RECORD_WEEKLY_LOOKBACK", "REQUIRED", true
                            )
                    );
        }

        @Test
        @DisplayName("첫걸음 Item은 bound Mailbox reward용 비장비 stack 99 정의다")
        void loadsFirstStepItemContract() {
            assertThat(jdbcTemplate.queryForMap("""
                    SELECT category, type, equipment_compatibility_kind,
                           stackable, max_stack, max_durability
                    FROM items
                    WHERE code = 'IT_FIRST_STEP_FRAGMENT'
                    """))
                    .containsEntry("category", "QUEST")
                    .containsEntry("type", "ETC")
                    .containsEntry("equipment_compatibility_kind", null)
                    .containsEntry("stackable", true)
                    .containsEntry("max_stack", 99)
                    .containsEntry("max_durability", null);
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM shop_items shop
                    JOIN items item ON item.id = shop.item_id
                    WHERE item.code = 'IT_FIRST_STEP_FRAGMENT'
                    """, Integer.class)).isZero();
        }

        @Test
        @DisplayName("V33의 active 10개와 eager onboarding 9개 slot은 그대로다")
        void preservesEquipmentSlotAuthority() {
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM equipment_slots
                    WHERE definition_version = '1.0.0'
                      AND enabled = b'1'
                      AND lifecycle_status = 'ACTIVE'
                    """, Integer.class)).isEqualTo(10);
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM equipment_slots
                    WHERE definition_version = '1.0.0'
                      AND eager_on_link_start = b'1'
                    """, Integer.class)).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("승인 Settlement Aggregate를 저장하고 상세 조회할 때")
    class PersistSettlementAggregate {

        @Test
        @DisplayName("Line을 함께 저장하고 같은 식별자의 재호출은 기존 Settlement를 반환한다")
        void persistsAndReturnsExistingSettlement() {
            var first = rewardSettlementCreateService.create(
                    185L,
                    RewardSettlementSourceType.QUEST_COMPLETION,
                    185001L,
                    "RP_EXP_TINY_10"
            );
            var second = rewardSettlementCreateService.create(
                    185L,
                    RewardSettlementSourceType.QUEST_COMPLETION,
                    185001L,
                    "RP_EXP_TINY_10"
            );
            var loaded = rewardSettlementReader.getByIdOrThrow(first.getId());

            assertThat(second.getId()).isEqualTo(first.getId());
            assertThat(loaded.getLines()).hasSize(1);
            assertThat(loaded.getLines().getFirst().getRewardDefinitionCode())
                    .isEqualTo("EXP_PLAYER");
            assertThat(loaded.getLines().getFirst().getAmount()).isEqualTo(10L);
        }
    }

    private record RouteStepRow(
            String code,
            int order,
            String title,
            String description,
            String questCode,
            String requirementType,
            boolean userAdvanceRequired
    ) {
    }
}
