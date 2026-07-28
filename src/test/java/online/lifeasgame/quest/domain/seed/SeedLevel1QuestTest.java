package online.lifeasgame.quest.domain.seed;

import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestRepeatRule;
import online.lifeasgame.quest.domain.QuestSemanticCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("Content 1B Seed Level 1 Quest Catalog")
class SeedLevel1QuestTest {

    @Test
    @DisplayName("공식 5개 code와 공통 계약을 sortOrder 순서로 보존한다")
    void preservesOfficialLedgerAndCommonContract() {
        List<SeedLevel1QuestDefinition> definitions =
                SeedLevel1Quest.definitions();

        assertThat(definitions).hasSize(5);
        assertThat(definitions)
                .extracting(SeedLevel1QuestDefinition::questCode)
                .containsExactly(
                        QuestCode.Q_RECORD_FIRST_TRACE,
                        QuestCode.Q_RECORD_THREE_TRACES,
                        QuestCode.Q_RECORD_WEEKLY_LOOKBACK,
                        QuestCode.Q_GROWTH_ONE_FOCUS,
                        QuestCode.Q_RECOVERY_REST_TEN
                )
                .doesNotHaveDuplicates();
        assertThat(definitions)
                .extracting(SeedLevel1QuestDefinition::sortOrder)
                .containsExactly(10, 20, 30, 40, 50)
                .doesNotHaveDuplicates();
        assertThat(definitions)
                .allSatisfy(definition -> {
                    assertThat(definition.seedLevel()).isEqualTo(1);
                    assertThat(definition.priority())
                            .isEqualTo(QuestContentPriority.P0);
                    assertThat(definition.definitionVersion()).isEqualTo(1);
                    assertThat(definition.status())
                            .isEqualTo(QuestDefinitionStatus.ACTIVE);
                    assertThat(definition.allowedRoleTypes())
                            .containsExactly("ANY");
                    assertThat(definition.manualCheckRequiresMemo()).isFalse();
                });
    }

    @Test
    @DisplayName("Q_RECORD_FIRST_TRACE의 Content 1B 원문 계약을 보존한다")
    void preservesFirstTraceContract() {
        SeedLevel1QuestDefinition definition =
                SeedLevel1Quest.RECORD_FIRST_TRACE.definition();

        assertSoftly(softly -> {
            softly.assertThat(definition.displayNameKo())
                    .isEqualTo("첫 흔적 남기기");
            softly.assertThat(definition.shortDescriptionKo())
                    .isEqualTo("오늘의 생각·행동·기억 중 하나를 짧게 남겨보세요.");
            softly.assertThat(definition.longDescriptionKo())
                    .isEqualTo(
                            "사용자가 직접 작성한 LifeLog 한 건을 남기면 완료됩니다. "
                                    + "Quick LifeLog도 실제 LifeLog로 저장되고 사용자 작성 "
                                    + "기록이라는 조건을 충족하면 인정합니다."
                    );
            assertRecordFact(softly, definition);
            softly.assertThat(definition.sourceOwnerRule()).isEqualTo(
                    "event.playerId == questAcceptance.playerId; "
                            + "event.occurredAt >= acceptance.acceptedAt"
            );
            softly.assertThat(definition.targetValue()).isEqualTo(1);
            softly.assertThat(definition.targetUnit())
                    .isEqualTo(QuestContentTargetUnit.DISTINCT_LIFELOG);
            softly.assertThat(definition.repeatPolicy())
                    .isEqualTo(QuestRepeatRule.ONCE);
            softly.assertThat(definition.periodBoundary())
                    .isEqualTo(QuestContentPeriodBoundary.PLAYER_LIFETIME);
            assertAutomaticNoTimezone(softly, definition);
            softly.assertThat(definition.rewardProfileCode())
                    .isEqualTo("RP_EXP_TINY_10");
            softly.assertThat(definition.cancellationPolicy()).isEqualTo(
                    "완료 전 취소와 새 attempt 허용; 한 번 완료되면 재수락 불가"
            );
            softly.assertThat(definition.failurePressurePolicy())
                    .isEqualTo("미완료/취소 페널티 없음");
            softly.assertThat(definition.notes()).containsExactly(
                    "허용 subtype의 사용자 작성 LifeLogRecorded 1건에서 자동 완료",
                    "동일 eventId 또는 lifeLogId 재전달 무시"
            );
            assertPresentation(
                    softly,
                    definition,
                    "icon.quest.record.first_trace",
                    "quest.record.primary",
                    "quest.q_record_first_trace.completed",
                    "quest.q_record_first_trace.empty"
            );
        });
    }

    @Test
    @DisplayName("Q_RECORD_THREE_TRACES의 distinct LifeLog 계약을 보존한다")
    void preservesThreeTracesContract() {
        SeedLevel1QuestDefinition definition =
                SeedLevel1Quest.RECORD_THREE_TRACES.definition();

        assertSoftly(softly -> {
            softly.assertThat(definition.displayNameKo())
                    .isEqualTo("흔적 세 개 이어보기");
            softly.assertThat(definition.shortDescriptionKo()).isEqualTo(
                    "서로 다른 순간의 기록을 세 개 남겨 작은 흐름을 만들어보세요."
            );
            softly.assertThat(definition.longDescriptionKo()).isEqualTo(
                    "수락 이후 사용자가 직접 남긴 서로 다른 LifeLog 세 건을 연결합니다. "
                            + "수정은 새 기록으로 보지 않으며 중복 전달은 진행도를 올리지 "
                            + "않습니다."
            );
            assertRecordFact(softly, definition);
            softly.assertThat(definition.sourceOwnerRule()).isEqualTo(
                    "event.playerId == questAcceptance.playerId; "
                            + "event.occurredAt >= acceptance.acceptedAt"
            );
            softly.assertThat(definition.targetValue()).isEqualTo(3);
            softly.assertThat(definition.targetUnit())
                    .isEqualTo(QuestContentTargetUnit.DISTINCT_LIFELOG);
            softly.assertThat(definition.repeatPolicy())
                    .isEqualTo(QuestRepeatRule.ONCE);
            softly.assertThat(definition.periodBoundary()).isEqualTo(
                    QuestContentPeriodBoundary
                            .FROM_ACCEPTANCE_UNTIL_COMPLETION
            );
            assertAutomaticNoTimezone(softly, definition);
            softly.assertThat(definition.rewardProfileCode())
                    .isEqualTo("RP_EXP_AND_ITEM_FIRST_STEP_20");
            assertPresentation(
                    softly,
                    definition,
                    "icon.quest.record.three_traces",
                    "quest.record.secondary",
                    "quest.q_record_three_traces.completed",
                    "quest.q_record_three_traces.empty"
            );
        });
    }

    @Test
    @DisplayName("Q_RECORD_WEEKLY_LOOKBACK의 주간 회고와 timezone 계약을 보존한다")
    void preservesWeeklyLookbackContract() {
        SeedLevel1QuestDefinition definition =
                SeedLevel1Quest.RECORD_WEEKLY_LOOKBACK.definition();

        assertSoftly(softly -> {
            softly.assertThat(definition.displayNameKo())
                    .isEqualTo("이번 주 흔적 돌아보기");
            softly.assertThat(definition.shortDescriptionKo()).isEqualTo(
                    "이번 주 기록 중 하나를 골라 지금의 나에게 남길 한 줄을 적어보세요."
            );
            softly.assertThat(definition.longDescriptionKo()).isEqualTo(
                    "별도 회고 전용 Entity를 만들지 않고 LifeLog subtype REFLECTION과 "
                            + "reflectionScope WEEKLY_LOOKBACK 메타데이터로 주간 회고를 "
                            + "식별합니다."
            );
            assertRecordFact(softly, definition);
            softly.assertThat(definition.sourceOwnerRule()).isEqualTo(
                    "event.playerId == questAcceptance.playerId; "
                            + "event.subtype == REFLECTION; "
                            + "event.reflectionScope == WEEKLY_LOOKBACK; "
                            + "event.periodKey == acceptance.periodKey"
            );
            softly.assertThat(definition.targetValue()).isEqualTo(1);
            softly.assertThat(definition.targetUnit())
                    .isEqualTo(QuestContentTargetUnit.WEEKLY_REFLECTION);
            softly.assertThat(definition.repeatPolicy())
                    .isEqualTo(QuestRepeatRule.WEEKLY);
            softly.assertThat(definition.periodBoundary()).isEqualTo(
                    QuestContentPeriodBoundary
                            .MONDAY_00_00_INCLUSIVE_TO_NEXT_MONDAY_00_00_EXCLUSIVE
            );
            softly.assertThat(definition.periodBoundary().value()).isEqualTo(
                    "MONDAY_00:00_INCLUSIVE_TO_NEXT_MONDAY_00:00_EXCLUSIVE"
            );
            assertAutomaticPlayerTimezone(softly, definition);
            softly.assertThat(definition.rewardProfileCode())
                    .isEqualTo("RP_NONE");
            assertPresentation(
                    softly,
                    definition,
                    "icon.quest.record.weekly_lookback",
                    "quest.record.reflection",
                    "quest.q_record_weekly_lookback.completed",
                    "quest.q_record_weekly_lookback.empty"
            );
        });
    }

    @Test
    @DisplayName("Q_GROWTH_ONE_FOCUS의 manual focus 정책을 보존한다")
    void preservesGrowthFocusContract() {
        SeedLevel1QuestDefinition definition =
                SeedLevel1Quest.GROWTH_ONE_FOCUS.definition();

        assertSoftly(softly -> {
            softly.assertThat(definition.displayNameKo())
                    .isEqualTo("한 가지에 25분 집중하기");
            softly.assertThat(definition.shortDescriptionKo())
                    .isEqualTo("지금 가장 작은 한 가지를 골라 25분만 집중해보세요.");
            softly.assertThat(definition.longDescriptionKo()).isEqualTo(
                    "사용자는 시작 전에 집중할 한 가지를 1~80자 자유 텍스트로 입력하고, "
                            + "집중을 마친 뒤 직접 완료를 확인합니다. 서버는 집중 내용이나 "
                            + "성과를 평가하지 않습니다."
            );
            assertManualConfirmation(softly, definition);
            softly.assertThat(definition.semanticCategory())
                    .isEqualTo(QuestSemanticCategory.GROWTH);
            softly.assertThat(definition.targetValue()).isEqualTo(25);
            softly.assertThat(definition.repeatPolicy())
                    .isEqualTo(QuestRepeatRule.DAILY);
            softly.assertThat(definition.rewardProfileCode())
                    .isEqualTo("RP_NONE");
            softly.assertThat(definition.notes()).containsExactly(
                    "Focus enum 신설 금지",
                    "focusLabel은 후속 Manual API에서 1~80자 필수",
                    "추천 Role은 optional이며 현재 single roleTemplateCode로 축약 금지"
            );
            assertPresentation(
                    softly,
                    definition,
                    "icon.quest.growth.one_focus",
                    "quest.growth.primary",
                    "quest.q_growth_one_focus.completed",
                    "quest.q_growth_one_focus.empty"
            );
        });
    }

    @Test
    @DisplayName("Q_RECOVERY_REST_TEN의 manual recovery와 무압박 정책을 보존한다")
    void preservesRecoveryRestContract() {
        SeedLevel1QuestDefinition definition =
                SeedLevel1Quest.RECOVERY_REST_TEN.definition();

        assertSoftly(softly -> {
            softly.assertThat(definition.displayNameKo())
                    .isEqualTo("10분 쉬어가기");
            softly.assertThat(definition.shortDescriptionKo())
                    .isEqualTo("아무것도 증명하지 않아도 되는 10분을 가져보세요.");
            softly.assertThat(definition.longDescriptionKo()).isEqualTo(
                    "사용자가 10분 정도 쉬었다고 직접 확인하면 완료합니다. P0에서는 "
                            + "Timer, Exercise, LifeLog를 필수 원본으로 요구하지 않습니다."
            );
            assertManualConfirmation(softly, definition);
            softly.assertThat(definition.semanticCategory())
                    .isEqualTo(QuestSemanticCategory.RECOVERY);
            softly.assertThat(definition.targetValue()).isEqualTo(10);
            softly.assertThat(definition.repeatPolicy())
                    .isEqualTo(QuestRepeatRule.DAILY);
            softly.assertThat(definition.rewardProfileCode())
                    .isEqualTo("RP_NONE");
            softly.assertThat(definition.failurePressurePolicy())
                    .isEqualTo("미완료 불이익/연속 수행 압박 금지");
            softly.assertThat(definition.notes()).containsExactly(
                    "Timer 증빙 강제 금지",
                    "부분 시간 누적 금지",
                    "미완료 불이익/연속 수행 압박 금지"
            );
            assertPresentation(
                    softly,
                    definition,
                    "icon.quest.recovery.rest_ten",
                    "quest.recovery.calm",
                    "quest.q_recovery_rest_ten.completed",
                    "quest.q_recovery_rest_ten.empty"
            );
        });
    }

    @Test
    @DisplayName("Catalog와 collection metadata는 외부에서 변경할 수 없다")
    void isImmutable() {
        SeedLevel1QuestDefinition definition =
                SeedLevel1Quest.RECORD_FIRST_TRACE.definition();

        assertThatThrownBy(() ->
                SeedLevel1Quest.definitions().add(definition)
        ).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() ->
                definition.allowedRoleTypes().add("ROLE")
        ).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() ->
                definition.notes().add("changed")
        ).isInstanceOf(UnsupportedOperationException.class);
    }

    private void assertRecordFact(
            org.assertj.core.api.SoftAssertions softly,
            SeedLevel1QuestDefinition definition
    ) {
        softly.assertThat(definition.semanticCategory())
                .isEqualTo(QuestSemanticCategory.RECORD);
        softly.assertThat(definition.progressMode())
                .isEqualTo(QuestProgressMode.EVENT_COUNT);
        softly.assertThat(definition.progressSourceType())
                .isEqualTo(QuestProgressSourceType.DURABLE_OUTBOX_FACT);
        softly.assertThat(definition.sourceEventType())
                .isEqualTo("LifeLogRecorded");
        softly.assertThat(definition.sourceEntityType())
                .isEqualTo("LIFE_LOG");
    }

    private void assertAutomaticNoTimezone(
            org.assertj.core.api.SoftAssertions softly,
            SeedLevel1QuestDefinition definition
    ) {
        softly.assertThat(definition.autoComplete()).isTrue();
        softly.assertThat(definition.manualCheckAllowed()).isFalse();
        softly.assertThat(definition.timezonePolicy())
                .isEqualTo(QuestContentTimezonePolicy.NOT_APPLICABLE);
        softly.assertThat(definition.timezoneFallback()).isNull();
        softly.assertThat(definition.roleContextPolicy()).isEqualTo(
                QuestRoleContextPolicy.OPTIONAL_SOURCE_ROLE_CONTEXT
        );
    }

    private void assertAutomaticPlayerTimezone(
            org.assertj.core.api.SoftAssertions softly,
            SeedLevel1QuestDefinition definition
    ) {
        softly.assertThat(definition.autoComplete()).isTrue();
        softly.assertThat(definition.manualCheckAllowed()).isFalse();
        softly.assertThat(definition.timezonePolicy())
                .isEqualTo(QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE);
        softly.assertThat(definition.timezoneFallback())
                .isEqualTo("Asia/Seoul");
        softly.assertThat(definition.roleContextPolicy()).isEqualTo(
                QuestRoleContextPolicy.OPTIONAL_SOURCE_ROLE_CONTEXT
        );
    }

    private void assertManualConfirmation(
            org.assertj.core.api.SoftAssertions softly,
            SeedLevel1QuestDefinition definition
    ) {
        softly.assertThat(definition.progressMode())
                .isEqualTo(QuestProgressMode.MANUAL_CHECK);
        softly.assertThat(definition.progressSourceType())
                .isEqualTo(QuestProgressSourceType.USER_CONFIRMATION);
        softly.assertThat(definition.sourceEventType())
                .isEqualTo("NONE_DIRECT_COMMAND");
        softly.assertThat(definition.sourceEntityType())
                .isEqualTo("QUEST_ACCEPTANCE");
        softly.assertThat(definition.sourceOwnerRule())
                .isEqualTo("command.playerId == questAcceptance.playerId");
        softly.assertThat(definition.targetUnit())
                .isEqualTo(QuestContentTargetUnit.MINUTE_INTENT);
        softly.assertThat(definition.autoComplete()).isFalse();
        softly.assertThat(definition.manualCheckAllowed()).isTrue();
        softly.assertThat(definition.periodBoundary()).isEqualTo(
                QuestContentPeriodBoundary.LOCAL_DAY_00_00_TO_NEXT_DAY_00_00
        );
        softly.assertThat(definition.periodBoundary().value())
                .isEqualTo("LOCAL_DAY_00:00_TO_NEXT_DAY_00:00");
        softly.assertThat(definition.timezonePolicy())
                .isEqualTo(QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE);
        softly.assertThat(definition.timezoneFallback())
                .isEqualTo("Asia/Seoul");
        softly.assertThat(definition.roleContextPolicy()).isEqualTo(
                QuestRoleContextPolicy.OPTIONAL_RECOMMENDED_ROLE_CONTEXT
        );
    }

    private void assertPresentation(
            org.assertj.core.api.SoftAssertions softly,
            SeedLevel1QuestDefinition definition,
            String iconKey,
            String colorToken,
            String resultCopyKey,
            String emptyStateCopyKey
    ) {
        softly.assertThat(definition.iconKey()).isEqualTo(iconKey);
        softly.assertThat(definition.colorToken()).isEqualTo(colorToken);
        softly.assertThat(definition.resultCopyKey()).isEqualTo(resultCopyKey);
        softly.assertThat(definition.emptyStateCopyKey())
                .isEqualTo(emptyStateCopyKey);
        softly.assertThat(definition.recommendedNextAction()).isNull();
    }
}
