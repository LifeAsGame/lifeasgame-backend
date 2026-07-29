package online.lifeasgame.quest.application.blueprint;

import online.lifeasgame.quest.domain.QuestBlueprint;
import online.lifeasgame.quest.domain.QuestCategory;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestCompletionPolicy;
import online.lifeasgame.quest.domain.QuestProgressSource;
import online.lifeasgame.quest.domain.QuestRepeatRule;
import online.lifeasgame.quest.domain.QuestSemanticCategory;
import online.lifeasgame.quest.domain.QuestTargetType;
import online.lifeasgame.quest.domain.seed.SeedLevel1Quest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("Content 1B Seed Level 1 Quest Blueprint adapter")
class SeedLevel1QuestBlueprintAdapterTest {

    @Test
    @DisplayName("공식 target/source/completion/repeat/reward를 Runtime 계약으로 투영한다")
    void mapsContentContractToRuntimeBlueprints() {
        Map<QuestCode, QuestBlueprint> blueprints =
                SeedLevel1Quest.definitions().stream()
                        .map(SeedLevel1QuestBlueprintAdapter::toBlueprint)
                        .collect(Collectors.toMap(
                                QuestBlueprint::code,
                                Function.identity()
                        ));

        assertThat(blueprints.values())
                .extracting(
                        QuestBlueprint::code,
                        blueprint -> blueprint.target().type(),
                        blueprint -> blueprint.target().value(),
                        QuestBlueprint::progressSource,
                        QuestBlueprint::completionPolicy,
                        QuestBlueprint::repeatPolicy,
                        QuestBlueprint::rewardProfileCodeOrNull
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                QuestCode.Q_RECORD_FIRST_TRACE,
                                QuestTargetType.COUNT,
                                1,
                                QuestProgressSource.RECORD_CREATED,
                                QuestCompletionPolicy.AUTO,
                                QuestRepeatRule.ONCE,
                                "RP_EXP_TINY_10"
                        ),
                        tuple(
                                QuestCode.Q_RECORD_THREE_TRACES,
                                QuestTargetType.COUNT,
                                3,
                                QuestProgressSource.RECORD_CREATED,
                                QuestCompletionPolicy.AUTO,
                                QuestRepeatRule.ONCE,
                                "RP_EXP_AND_ITEM_FIRST_STEP_20"
                        ),
                        tuple(
                                QuestCode.Q_RECORD_WEEKLY_LOOKBACK,
                                QuestTargetType.COUNT,
                                1,
                                QuestProgressSource.RECORD_CREATED,
                                QuestCompletionPolicy.AUTO,
                                QuestRepeatRule.WEEKLY,
                                "RP_NONE"
                        ),
                        tuple(
                                QuestCode.Q_GROWTH_ONE_FOCUS,
                                QuestTargetType.MINUTES,
                                25,
                                QuestProgressSource.MANUAL_CHECK,
                                QuestCompletionPolicy.USER_CONFIRM,
                                QuestRepeatRule.DAILY,
                                "RP_NONE"
                        ),
                        tuple(
                                QuestCode.Q_RECOVERY_REST_TEN,
                                QuestTargetType.MINUTES,
                                10,
                                QuestProgressSource.MANUAL_CHECK,
                                QuestCompletionPolicy.USER_CONFIRM,
                                QuestRepeatRule.DAILY,
                                "RP_NONE"
                        )
                );
    }

    @Test
    @DisplayName("신규 final Blueprint와 materialized Quest에 legacy category/Role을 만들지 않는다")
    void keepsLegacyCategoryAndRoleNullForFinalContracts() {
        assertThat(SeedLevel1Quest.definitions())
                .map(SeedLevel1QuestBlueprintAdapter::toBlueprint)
                .allSatisfy(blueprint -> {
                    assertThat(blueprint.isFinalContract()).isTrue();
                    assertThat(blueprint.category()).isNull();
                    assertThat(blueprint.roleTemplateRef()).isNull();

                    var quest = blueprint.instantiate();
                    assertThat(quest.getCategory()).isNull();
                    assertThat(quest.getSemanticCategory()).isNotNull();
                    assertThat(quest.roleTemplateCodeOrNull()).isNull();
                });
    }

    @Test
    @DisplayName("Static Catalog는 legacy를 유지하고 신규 5개를 sortOrder 순서로 제공한다")
    void extendsStaticCatalogWithoutReinterpretingLegacyBlueprints() {
        StaticQuestBlueprintCatalog catalog =
                new StaticQuestBlueprintCatalog();
        Set<QuestCode> seedCodes = SeedLevel1Quest.definitions().stream()
                .map(definition -> definition.questCode())
                .collect(Collectors.toSet());

        assertThat(catalog.all().stream()
                .filter(blueprint -> seedCodes.contains(blueprint.code())))
                .extracting(QuestBlueprint::code)
                .containsExactly(
                        QuestCode.Q_RECORD_FIRST_TRACE,
                        QuestCode.Q_RECORD_THREE_TRACES,
                        QuestCode.Q_RECORD_WEEKLY_LOOKBACK,
                        QuestCode.Q_GROWTH_ONE_FOCUS,
                        QuestCode.Q_RECOVERY_REST_TEN
                );

        QuestBlueprint legacy = catalog.require(QuestCode.PLAYER_WELCOME);
        assertThat(legacy.category()).isEqualTo(QuestCategory.MAIN);
        assertThat(legacy.semanticCategory()).isNull();
        assertThat(legacy.progressSource()).isNull();
    }

    @Test
    @DisplayName("공식 enum name과 stable value를 그대로 parse한다")
    void parsesOfficialStableCodes() {
        assertThat(SeedLevel1Quest.definitions())
                .allSatisfy(definition -> {
                    String stableCode = definition.questCode().value();
                    assertThat(stableCode)
                            .isEqualTo(definition.questCode().name());
                    assertThat(QuestCode.parse(stableCode))
                            .isEqualTo(definition.questCode());
                });
    }

    @Test
    @DisplayName("semantic category는 Content 1B 분류를 그대로 유지한다")
    void keepsSemanticCategory() {
        assertThat(SeedLevel1Quest.definitions())
                .map(SeedLevel1QuestBlueprintAdapter::toBlueprint)
                .extracting(QuestBlueprint::semanticCategory)
                .containsExactly(
                        QuestSemanticCategory.RECORD,
                        QuestSemanticCategory.RECORD,
                        QuestSemanticCategory.RECORD,
                        QuestSemanticCategory.GROWTH,
                        QuestSemanticCategory.RECOVERY
                );
    }
}
