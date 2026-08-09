package online.lifeasgame.quest.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Quest semantic Definition 계약")
class QuestSemanticDefinitionContractTest {

    @Nested
    @DisplayName("final-contract Definition을 생성할 때")
    class CreateFinalContract {

        @Test
        @DisplayName("semantic/progress/repeat과 선택적 Role code를 보존한다")
        void createsFinalDefinition() {
            Quest quest = finalQuest(QuestRoleTemplateRef.of("  ROLE_WARRIOR  "));

            assertThat(quest.isFinalContract()).isTrue();
            assertThat(quest.getSemanticCategory())
                    .isEqualTo(QuestSemanticCategory.GROWTH);
            assertThat(quest.getProgressSource())
                    .isEqualTo(QuestProgressSource.COUNT);
            assertThat(quest.getRepeatRule()).isEqualTo(QuestRepeatRule.ONCE);
            assertThat(quest.repeatPolicyOrNull())
                    .isEqualTo(QuestRepeatRule.ONCE);
            assertThat(quest.roleTemplateCodeOrNull())
                    .isEqualTo("ROLE_WARRIOR");
        }

        @Test
        @DisplayName("final-contract는 legacy category 없이 semantic category로 생성한다")
        void allowsNullLegacyCategory() {
            Quest quest = Quest.createDefinition(
                    "Q_TEST_FINAL_WITHOUT_LEGACY_CATEGORY",
                    1,
                    QuestSemanticCategory.RECORD,
                    QuestTitle.of("Final without legacy category"),
                    "final category contract",
                    QuestTarget.of(QuestTargetType.COUNT, 1),
                    QuestProgressSource.RECORD_CREATED,
                    RewardProfileRef.of("RP_NONE"),
                    QuestRepeatRule.ONCE,
                    null,
                    QuestCompletionPolicy.AUTO,
                    null
            );

            assertThat(quest.isFinalContract()).isTrue();
            assertThat(quest.getCategory()).isNull();
            assertThat(quest.getSemanticCategory())
                    .isEqualTo(QuestSemanticCategory.RECORD);
        }

        @Test
        @DisplayName("semantic category는 필수다")
        void requiresSemanticCategory() {
            assertQuestError(
                    () -> createFinal(
                            null,
                            QuestProgressSource.COUNT,
                            QuestRepeatRule.ONCE
                    ),
                    QuestError.QUEST_SEMANTIC_CATEGORY_REQUIRED
            );
        }

        @Test
        @DisplayName("progress source는 필수다")
        void requiresProgressSource() {
            assertQuestError(
                    () -> createFinal(
                            QuestSemanticCategory.GROWTH,
                            null,
                            QuestRepeatRule.ONCE
                    ),
                    QuestError.QUEST_PROGRESS_SOURCE_REQUIRED
            );
        }

        @Test
        @DisplayName("repeat policy는 필수이며 legacy MONTHLY를 허용하지 않는다")
        void requiresFinalRepeatPolicy() {
            assertQuestError(
                    () -> createFinal(
                            QuestSemanticCategory.GROWTH,
                            QuestProgressSource.COUNT,
                            null
                    ),
                    QuestError.QUEST_REPEAT_POLICY_REQUIRED
            );
            assertQuestError(
                    () -> createFinal(
                            QuestSemanticCategory.GROWTH,
                            QuestProgressSource.COUNT,
                            QuestRepeatRule.MONTHLY
                    ),
                    QuestError.INVALID_QUEST_REPEAT_POLICY
            );
        }
    }

    @Nested
    @DisplayName("enum과 Role code를 입력할 때")
    class ParseContract {

        @Test
        @DisplayName("semantic category와 progress source를 strict parse한다")
        void parsesStrictEnums() {
            assertThat(QuestSemanticCategory.parse(" growth "))
                    .isEqualTo(QuestSemanticCategory.GROWTH);
            assertThat(QuestProgressSource.parse("record-created"))
                    .isEqualTo(QuestProgressSource.RECORD_CREATED);

            assertQuestError(
                    () -> QuestSemanticCategory.parse("DAILY"),
                    QuestError.INVALID_QUEST_SEMANTIC_CATEGORY
            );
            assertQuestError(
                    () -> QuestProgressSource.parse("QuestEvent"),
                    QuestError.INVALID_QUEST_PROGRESS_SOURCE
            );
        }

        @Test
        @DisplayName("repeatPolicy는 ONCE/DAILY/WEEKLY만 parse한다")
        void parsesFinalRepeatPolicy() {
            assertThat(QuestRepeatRule.parsePolicy("once"))
                    .isEqualTo(QuestRepeatRule.ONCE);
            assertThat(QuestRepeatRule.parsePolicy("WEEKLY"))
                    .isEqualTo(QuestRepeatRule.WEEKLY);
            assertQuestError(
                    () -> QuestRepeatRule.parsePolicy("NONE"),
                    QuestError.INVALID_QUEST_REPEAT_POLICY
            );
            assertQuestError(
                    () -> QuestRepeatRule.parsePolicy("MONTHLY"),
                    QuestError.INVALID_QUEST_REPEAT_POLICY
            );
        }

        @Test
        @DisplayName("Role code는 trim하고 blank와 80자 초과를 거부한다")
        void validatesRoleTemplateCode() {
            assertThat(QuestRoleTemplateRef.of("  ROLE_READER  ").code())
                    .isEqualTo("ROLE_READER");
            assertQuestError(
                    () -> QuestRoleTemplateRef.of("   "),
                    QuestError.QUEST_ROLE_TEMPLATE_CODE_REQUIRED
            );
            assertQuestError(
                    () -> QuestRoleTemplateRef.of("R".repeat(81)),
                    QuestError.QUEST_ROLE_TEMPLATE_CODE_TOO_LONG
            );
        }
    }

    @Nested
    @DisplayName("final-contract Definition을 수정할 때")
    class UpdateFinalContract {

        @Test
        @DisplayName("동일 값 partial update는 Event 없는 no-op이다")
        void keepsSameValueNoOp() {
            Quest quest = finalQuest(QuestRoleTemplateRef.of("ROLE_WARRIOR"));

            boolean changed = quest.updateDefinition(
                    quest.target(),
                    null,
                    QuestRepeatRule.ONCE,
                    null,
                    2,
                    RewardProfileRef.of("RP_EXP_30"),
                    QuestSemanticCategory.GROWTH,
                    QuestProgressSource.COUNT,
                    QuestRepeatRule.ONCE,
                    QuestRoleTemplateRef.of(" ROLE_WARRIOR ")
            );

            assertThat(changed).isFalse();
        }

        @Test
        @DisplayName("version validation 실패는 다른 Definition mutation보다 먼저 끝난다")
        void validatesBeforeMutation() {
            Quest quest = finalQuest(null);

            assertQuestError(
                    () -> quest.updateDefinition(
                            QuestTarget.of(QuestTargetType.COUNT, 9),
                            null,
                            null,
                            null,
                            1,
                            null,
                            QuestSemanticCategory.RECOVERY,
                            QuestProgressSource.MANUAL_CHECK,
                            QuestRepeatRule.DAILY,
                            QuestRoleTemplateRef.of("ROLE_RECOVERY")
                    ),
                    QuestError.QUEST_DEFINITION_VERSION_DECREASE_NOT_ALLOWED
            );

            assertThat(quest.target().value()).isEqualTo(1);
            assertThat(quest.getSemanticCategory())
                    .isEqualTo(QuestSemanticCategory.GROWTH);
            assertThat(quest.getProgressSource())
                    .isEqualTo(QuestProgressSource.COUNT);
            assertThat(quest.getRepeatRule()).isEqualTo(QuestRepeatRule.ONCE);
            assertThat(quest.roleTemplateCodeOrNull()).isNull();
        }
    }

    @Test
    @DisplayName("legacy 생성은 semantic/progress/repeatPolicy/Role을 추론하지 않는다")
    void keepsLegacyCreationUninterpreted() {
        Quest quest = Quest.create(
                "quest:test:legacy-semantic",
                QuestCategory.REPEAT,
                QuestTitle.of("Legacy Repeat"),
                "legacy category를 의미 분류로 해석하지 않는다",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.MONTHLY,
                QuestCompletionPolicy.AUTO,
                null
        );

        assertThat(quest.isFinalContract()).isFalse();
        assertThat(quest.getSemanticCategory()).isNull();
        assertThat(quest.getProgressSource()).isNull();
        assertThat(quest.repeatPolicyOrNull()).isNull();
        assertThat(quest.roleTemplateCodeOrNull()).isNull();
        assertThat(quest.getCategory()).isEqualTo(QuestCategory.REPEAT);
        assertThat(quest.getRepeatRule()).isEqualTo(QuestRepeatRule.MONTHLY);
    }

    @Test
    @DisplayName("legacy 생성은 기존 category를 계속 필수로 요구한다")
    void requiresLegacyCategory() {
        assertThatThrownBy(() -> Quest.create(
                "quest:test:legacy-without-category",
                null,
                QuestTitle.of("Legacy without category"),
                "invalid legacy",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("category must not be null");
    }

    private Quest finalQuest(QuestRoleTemplateRef roleTemplateRef) {
        return Quest.createDefinition(
                "quest:test:semantic-final",
                2,
                QuestCategory.MAIN,
                QuestSemanticCategory.GROWTH,
                QuestTitle.of("Semantic Final Quest"),
                "final contract",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestProgressSource.COUNT,
                RewardProfileRef.of("RP_EXP_30"),
                QuestRepeatRule.ONCE,
                roleTemplateRef,
                QuestCompletionPolicy.AUTO,
                null
        );
    }

    private Quest createFinal(
            QuestSemanticCategory semanticCategory,
            QuestProgressSource progressSource,
            QuestRepeatRule repeatPolicy
    ) {
        return Quest.createDefinition(
                "quest:test:required-final",
                2,
                QuestCategory.MAIN,
                semanticCategory,
                QuestTitle.of("Required Final Quest"),
                "required final contract",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                progressSource,
                RewardProfileRef.of("RP_EXP_10"),
                repeatPolicy,
                null,
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
