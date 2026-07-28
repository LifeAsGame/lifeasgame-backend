package online.lifeasgame.quest.domain.event;

import online.lifeasgame.quest.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestEvent Definition Snapshot")
class QuestEventDefinitionSnapshotTest {

    @Test
    @DisplayName("RewardProfile QuestCompleted는 version과 code만 보상 계약으로 기록한다")
    void snapshotsProfileDefinitionReference() {
        QuestEvent event = QuestEvent.snapshot(
                QuestEventType.QUEST_COMPLETED,
                profileQuest(),
                "quest:test:profile:completed"
        );

        assertThat(event.type()).isEqualTo(QuestEventType.QUEST_COMPLETED);
        assertThat(event.attributes())
                .containsEntry("questDefinitionVersion", 3)
                .containsEntry("questSemanticCategory", "GROWTH")
                .containsEntry("progressSource", "COUNT")
                .containsEntry("repeatPolicy", "ONCE")
                .containsEntry("roleTemplateCode", "ROLE_WARRIOR")
                .containsEntry("rewardProfileCode", "RP_EXP_30")
                .doesNotContainKeys(
                        "rewardExp",
                        "rewardStats",
                        "rewardLines",
                        "rewardProfileId"
                );
    }

    @Test
    @DisplayName("QUEST_UPDATED final Snapshot은 null Role code를 제외한다")
    void snapshotsUpdatedFinalDefinitionWithoutRole() {
        QuestEvent event = QuestEvent.snapshot(
                QuestEventType.QUEST_UPDATED,
                profileQuest(null),
                "quest:test:profile:updated"
        );

        assertThat(event.attributes())
                .containsEntry("questDefinitionVersion", 3)
                .containsEntry("questSemanticCategory", "GROWTH")
                .containsEntry("progressSource", "COUNT")
                .containsEntry("repeatPolicy", "ONCE")
                .containsEntry("rewardProfileCode", "RP_EXP_30")
                .doesNotContainKeys(
                        "roleTemplateCode",
                        "rewardExp",
                        "rewardStats",
                        "rewardLines",
                        "rewardProfileId"
                );
    }

    @Test
    @DisplayName("legacy QuestCompleted는 version 1과 inline reward Snapshot을 유지한다")
    void snapshotsLegacyDefinition() {
        QuestEvent event = QuestEvent.snapshot(
                QuestEventType.QUEST_COMPLETED,
                legacyQuest(),
                "quest:test:legacy:completed"
        );

        assertThat(event.type()).isEqualTo(QuestEventType.QUEST_COMPLETED);
        assertThat(event.attributes())
                .containsEntry("questDefinitionVersion", 1)
                .containsEntry("rewardExp", 7)
                .containsEntry("rewardStats", java.util.Map.of("strength", 2))
                .doesNotContainKeys(
                        "questSemanticCategory",
                        "progressSource",
                        "repeatPolicy",
                        "roleTemplateCode",
                        "rewardProfileCode",
                        "rewardLines",
                        "rewardProfileId"
                );
    }

    private Quest profileQuest() {
        return profileQuest(QuestRoleTemplateRef.of("ROLE_WARRIOR"));
    }

    private Quest profileQuest(QuestRoleTemplateRef roleTemplateRef) {
        return Quest.createDefinition(
                "quest:test:profile",
                3,
                QuestCategory.MAIN,
                QuestSemanticCategory.GROWTH,
                QuestTitle.of("Profile Quest"),
                "profile",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestProgressSource.COUNT,
                RewardProfileRef.of("RP_EXP_30"),
                QuestRepeatRule.ONCE,
                roleTemplateRef,
                QuestCompletionPolicy.AUTO,
                null
        );
    }

    private Quest legacyQuest() {
        return Quest.create(
                "quest:test:legacy",
                QuestCategory.MAIN,
                QuestTitle.of("Legacy Quest"),
                "legacy",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(
                        7,
                        new RewardStats(java.util.Map.of("strength", 2))
                ),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.AUTO,
                null
        );
    }
}
