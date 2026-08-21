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

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("V25 migration 이후 JPA schema validation")
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
    @DisplayName("V1부터 V25까지 적용된 schema로 ApplicationContext를 기동할 때")
    class LoadApplicationContext {

        @Test
        @DisplayName("ddl-auto validate 상태로 정상 기동한다")
        void loadsWithJpaValidation() {
            assertThat(applicationContext).isNotNull();
            assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("25");
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
                    .containsExactly(
                            "RP_EXP_10",
                            "RP_EXP_30",
                            "RP_EXP_AND_ITEM_FIRST_STEP_20",
                            "RP_EXP_TINY_10",
                            "RP_NONE"
                    );
        }
    }

    @Nested
    @DisplayName("V13 공식 TINY Reward Profile을 조회할 때")
    class LoadTinyExpRewardProfile {

        @Test
        @DisplayName("기존 RD_EXP_10을 참조하는 ACTIVE EXP 10 line을 반환한다")
        void loadsActiveTinyProfileWithLegacyDefinition() {
            var reference = rewardProfileLookupApi.getActiveByCode("RP_EXP_TINY_10");
            var detail = rewardProfileQueryService.getProfileView("RP_EXP_TINY_10");

            assertThat(reference.code()).isEqualTo("RP_EXP_TINY_10");
            assertThat(detail.code()).isEqualTo("RP_EXP_TINY_10");
            assertThat(detail.name()).isEqualTo("소량 EXP");
            assertThat(detail.status()).isEqualTo("ACTIVE");
            assertThat(detail.lines()).hasSize(1);

            RewardProfileResult.Line line = detail.lines().getFirst();
            assertThat(line.sortOrder()).isZero();
            assertThat(line.amountOverride()).isNull();
            assertThat(line.effectiveAmount()).isEqualTo(10L);
            assertThat(line.rewardDefinition().code()).isEqualTo("RD_EXP_10");
            assertThat(line.rewardDefinition().rewardType()).isEqualTo("EXP");
            assertThat(line.rewardDefinition().amount()).isEqualTo(10L);
            assertThat(line.rewardDefinition().itemId()).isNull();
        }
    }

    @Nested
    @DisplayName("V13 first-step Reward Profile을 조회할 때")
    class LoadFirstStepRewardProfile {

        @Test
        @DisplayName("기존 lookup과 상세 조회로 EXP 20 및 stable Item x1을 반환한다")
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
            assertThat(expLine.sortOrder()).isZero();
            assertThat(expLine.amountOverride()).isNull();
            assertThat(expLine.effectiveAmount()).isEqualTo(20L);
            assertThat(expLine.rewardDefinition().code()).isEqualTo("RD_EXP_20");
            assertThat(expLine.rewardDefinition().rewardType()).isEqualTo("EXP");
            assertThat(expLine.rewardDefinition().amount()).isEqualTo(20L);
            assertThat(expLine.rewardDefinition().itemId()).isNull();

            RewardProfileResult.Line itemLine = detail.lines().get(1);
            assertThat(itemLine.sortOrder()).isEqualTo(1);
            assertThat(itemLine.amountOverride()).isNull();
            assertThat(itemLine.effectiveAmount()).isEqualTo(1L);
            assertThat(itemLine.rewardDefinition().code())
                    .isEqualTo("RD_ITEM_FIRST_STEP_FRAGMENT_1");
            assertThat(itemLine.rewardDefinition().rewardType()).isEqualTo("ITEM");
            assertThat(itemLine.rewardDefinition().amount()).isEqualTo(1L);
            assertThat(itemLine.rewardDefinition().itemId()).isEqualTo(item.id());
        }
    }

    @Nested
    @DisplayName("V3 Settlement Aggregate를 저장하고 상세 조회할 때")
    class PersistSettlementAggregate {

        @Test
        @DisplayName("Line을 함께 저장하고 같은 식별자의 재호출은 기존 Settlement를 반환한다")
        void persistsAndReturnsExistingSettlement() {
            var first = rewardSettlementCreateService.create(
                    185L,
                    RewardSettlementSourceType.QUEST_COMPLETION,
                    185001L,
                    "RP_EXP_10"
            );
            var second = rewardSettlementCreateService.create(
                    185L,
                    RewardSettlementSourceType.QUEST_COMPLETION,
                    185001L,
                    "RP_EXP_10"
            );
            var loaded = rewardSettlementReader.getByIdOrThrow(first.getId());

            assertThat(second.getId()).isEqualTo(first.getId());
            assertThat(loaded.getLines()).hasSize(1);
            assertThat(loaded.getLines().getFirst().getRewardDefinitionCode())
                    .isEqualTo("RD_EXP_10");
            assertThat(loaded.getLines().getFirst().getAmount()).isEqualTo(10L);
        }
    }
}
