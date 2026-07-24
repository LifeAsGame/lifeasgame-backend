package online.lifeasgame.quest.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Quest Definition version과 RewardProfile 참조 계약")
class QuestDefinitionContractTest {

    @Nested
    @DisplayName("RewardProfile 기반 Definition을 생성할 때")
    class CreateProfileDefinition {

        @Test
        @DisplayName("명시한 version과 trim된 profile code를 보존한다")
        void createsDefinition() {
            Quest quest = profileQuest(3, "  RP_EXP_30  ");

            assertThat(quest.getDefinitionVersion()).isEqualTo(3);
            assertThat(quest.rewardProfileCodeOrNull()).isEqualTo("RP_EXP_30");
            assertThat(quest.usesRewardProfile()).isTrue();
            assertThat(quest.isLegacyInlineReward()).isFalse();
        }

        @Test
        @DisplayName("version 1 미만은 안정된 QuestError로 거부한다")
        void rejectsInvalidVersion() {
            assertQuestError(
                    () -> profileQuest(0, "RP_EXP_10"),
                    QuestError.QUEST_DEFINITION_VERSION_INVALID
            );
        }

        @Test
        @DisplayName("blank 또는 80자를 넘는 profile code를 거부한다")
        void rejectsInvalidProfileCode() {
            assertQuestError(
                    () -> RewardProfileRef.of("   "),
                    QuestError.QUEST_REWARD_PROFILE_CODE_REQUIRED
            );
            assertQuestError(
                    () -> RewardProfileRef.of("R".repeat(81)),
                    QuestError.QUEST_REWARD_PROFILE_CODE_TOO_LONG
            );
        }
    }

    @Nested
    @DisplayName("Definition을 수정할 때")
    class UpdateDefinition {

        @Test
        @DisplayName("저장된 version보다 낮은 version은 mutation 전에 거부한다")
        void rejectsVersionDecrease() {
            Quest quest = profileQuest(3, "RP_EXP_30");

            assertQuestError(
                    () -> quest.updateDefinition(
                            QuestTarget.of(QuestTargetType.COUNT, 2),
                            null,
                            null,
                            null,
                            2,
                            null
                    ),
                    QuestError.QUEST_DEFINITION_VERSION_DECREASE_NOT_ALLOWED
            );

            assertThat(quest.getDefinitionVersion()).isEqualTo(3);
            assertThat(quest.target().value()).isEqualTo(1);
            assertThat(quest.pullEvents()).isEmpty();
        }

        @Test
        @DisplayName("profile ref와 inline reward 동시 변경을 거부한다")
        void rejectsMixedRewardContracts() {
            Quest quest = legacyQuest();

            assertQuestError(
                    () -> quest.updateDefinition(
                            null,
                            QuestReward.of(10, RewardStats.empty()),
                            null,
                            null,
                            2,
                            RewardProfileRef.of("RP_EXP_10")
                    ),
                    QuestError.QUEST_REWARD_CONTRACT_CONFLICT
            );

            assertThat(quest.getDefinitionVersion()).isEqualTo(1);
            assertThat(quest.rewardProfileCodeOrNull()).isNull();
            assertThat(quest.getReward().exp()).isZero();
            assertThat(quest.pullEvents()).isEmpty();
        }

        @Test
        @DisplayName("동일한 명시 값은 no-op이며 Event를 만들지 않는다")
        void doesNotRecordNoOpEvent() {
            Quest quest = profileQuest(2, "RP_EXP_10");

            quest.updateDefinition(
                    quest.target(),
                    null,
                    quest.getRepeatRule(),
                    null,
                    2,
                    RewardProfileRef.of("RP_EXP_10")
            );

            assertThat(quest.pullEvents()).isEmpty();
        }

        @Test
        @DisplayName("변경 Event에는 version과 profile code만 보상 Snapshot으로 담는다")
        void snapshotsDefinitionReference() {
            Quest quest = profileQuest(1, "RP_EXP_10");

            quest.updateDefinition(
                    null,
                    null,
                    null,
                    null,
                    2,
                    RewardProfileRef.of("RP_EXP_30")
            );

            QuestEvent event = (QuestEvent) quest.pullEvents().getFirst();
            assertThat(event.type()).isEqualTo(QuestEventType.QUEST_UPDATED);
            assertThat(event.attributes())
                    .containsEntry("questDefinitionVersion", 2)
                    .containsEntry("rewardProfileCode", "RP_EXP_30")
                    .doesNotContainKeys("rewardExp", "rewardStats", "rewardLines");
        }
    }

    @Test
    @DisplayName("legacy 생성 overload는 version 1과 inline reward를 유지한다")
    void keepsLegacyCreation() {
        Quest quest = legacyQuest();

        assertThat(quest.getDefinitionVersion()).isEqualTo(1);
        assertThat(quest.rewardProfileCodeOrNull()).isNull();
        assertThat(quest.isLegacyInlineReward()).isTrue();
        assertThat(quest.getReward().exp()).isZero();
    }

    private Quest profileQuest(int version, String profileCode) {
        return Quest.createDefinition(
                "quest:test:profile",
                version,
                QuestCategory.MAIN,
                QuestTitle.of("Profile Quest"),
                "RewardProfile 기반 Quest",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                RewardProfileRef.of(profileCode),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.AUTO,
                null
        );
    }

    private Quest legacyQuest() {
        return Quest.create(
                "quest:test:legacy",
                QuestCategory.MAIN,
                QuestTitle.of("Legacy Quest"),
                "inline reward 기반 Quest",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.AUTO,
                null
        );
    }

    private void assertQuestError(Runnable action, QuestError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
