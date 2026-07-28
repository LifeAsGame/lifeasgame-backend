package online.lifeasgame.quest.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestBlueprint semantic 계약")
class QuestBlueprintContractTest {

    @Test
    @DisplayName("final-contract Blueprint는 신규 Quest 생성 계약을 materialize한다")
    void instantiatesFinalContract() {
        QuestBlueprint blueprint = finalBlueprint(
                QuestRoleTemplateRef.of(" ROLE_READER ")
        );

        Quest quest = blueprint.instantiate();

        assertThat(blueprint.isFinalContract()).isTrue();
        assertThat(quest.isFinalContract()).isTrue();
        assertThat(quest.getSemanticCategory())
                .isEqualTo(QuestSemanticCategory.RECORD);
        assertThat(quest.getProgressSource())
                .isEqualTo(QuestProgressSource.RECORD_CREATED);
        assertThat(quest.getRepeatRule()).isEqualTo(QuestRepeatRule.DAILY);
        assertThat(quest.roleTemplateCodeOrNull()).isEqualTo("ROLE_READER");
        assertThat(quest.rewardProfileCodeOrNull()).isEqualTo("RP_EXP_10");
    }

    @Test
    @DisplayName("null Role code를 허용하고 Quest에도 null을 유지한다")
    void allowsNullRoleContext() {
        Quest quest = finalBlueprint(null).instantiate();

        assertThat(quest.isFinalContract()).isTrue();
        assertThat(quest.roleTemplateCodeOrNull()).isNull();
    }

    @Test
    @DisplayName("legacy Blueprint는 기존 category와 MONTHLY를 재해석하지 않는다")
    void instantiatesLegacyBlueprint() {
        QuestBlueprint blueprint = new QuestBlueprint(
                QuestCode.COLLECTION_HUNTER_10,
                QuestCategory.RECOMMENDED,
                QuestTitle.of("Legacy Blueprint"),
                "legacy blueprint",
                QuestTarget.of(QuestTargetType.COUNT, 10),
                QuestReward.of(3, RewardStats.empty()),
                QuestRepeatRule.MONTHLY,
                null
        );

        Quest quest = blueprint.instantiate();

        assertThat(blueprint.isFinalContract()).isFalse();
        assertThat(blueprint.semanticCategory()).isNull();
        assertThat(blueprint.progressSource()).isNull();
        assertThat(blueprint.repeatPolicy()).isNull();
        assertThat(quest.getCategory()).isEqualTo(QuestCategory.RECOMMENDED);
        assertThat(quest.getRepeatRule()).isEqualTo(QuestRepeatRule.MONTHLY);
        assertThat(quest.getSemanticCategory()).isNull();
        assertThat(quest.getProgressSource()).isNull();
    }

    private QuestBlueprint finalBlueprint(
            QuestRoleTemplateRef roleTemplateRef
    ) {
        return QuestBlueprint.finalContract(
                QuestCode.PLAYER_WELCOME,
                2,
                QuestCategory.MAIN,
                QuestSemanticCategory.RECORD,
                QuestTitle.of("Final Blueprint"),
                "final blueprint",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestProgressSource.RECORD_CREATED,
                RewardProfileRef.of("RP_EXP_10"),
                QuestRepeatRule.DAILY,
                roleTemplateRef,
                null,
                QuestCompletionPolicy.AUTO
        );
    }
}
