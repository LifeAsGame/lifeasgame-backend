package online.lifeasgame.quest.domain.seed;

import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestRepeatRule;
import online.lifeasgame.quest.domain.QuestSemanticCategory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum SeedLevel1Quest {

    RECORD_FIRST_TRACE(
            new SeedLevel1QuestDefinition(
                    QuestCode.Q_RECORD_FIRST_TRACE,
                    1,
                    QuestContentPriority.P0,
                    1,
                    QuestDefinitionStatus.ACTIVE,
                    "첫 흔적 남기기",
                    "오늘의 생각·행동·기억 중 하나를 짧게 남겨보세요.",
                    "사용자가 직접 작성한 LifeLog 한 건을 남기면 완료됩니다. "
                            + "Quick LifeLog도 실제 LifeLog로 저장되고 사용자 작성 "
                            + "기록이라는 조건을 충족하면 인정합니다.",
                    QuestSemanticCategory.RECORD,
                    QuestProgressMode.EVENT_COUNT,
                    QuestProgressSourceType.DURABLE_OUTBOX_FACT,
                    "LifeLogRecorded",
                    "LIFE_LOG",
                    "event.playerId == questAcceptance.playerId; "
                            + "event.occurredAt >= acceptance.acceptedAt",
                    1,
                    QuestContentTargetUnit.DISTINCT_LIFELOG,
                    true,
                    QuestRepeatRule.ONCE,
                    QuestContentPeriodBoundary.PLAYER_LIFETIME,
                    QuestContentTimezonePolicy.NOT_APPLICABLE,
                    null,
                    QuestRoleContextPolicy.OPTIONAL_SOURCE_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_EXP_TINY_10",
                    false,
                    false,
                    "완료 전 취소와 새 attempt 허용; 한 번 완료되면 재수락 불가",
                    "미완료/취소 페널티 없음",
                    null,
                    10,
                    "icon.quest.record.first_trace",
                    "quest.record.primary",
                    "quest.q_record_first_trace.completed",
                    "quest.q_record_first_trace.empty",
                    List.of(
                            "허용 subtype의 사용자 작성 LifeLogRecorded 1건에서 자동 완료",
                            "동일 eventId 또는 lifeLogId 재전달 무시"
                    )
            )
    ),

    RECORD_THREE_TRACES(
            new SeedLevel1QuestDefinition(
                    QuestCode.Q_RECORD_THREE_TRACES,
                    1,
                    QuestContentPriority.P0,
                    1,
                    QuestDefinitionStatus.ACTIVE,
                    "흔적 세 개 이어보기",
                    "서로 다른 순간의 기록을 세 개 남겨 작은 흐름을 만들어보세요.",
                    "수락 이후 사용자가 직접 남긴 서로 다른 LifeLog 세 건을 연결합니다. "
                            + "수정은 새 기록으로 보지 않으며 중복 전달은 진행도를 올리지 "
                            + "않습니다.",
                    QuestSemanticCategory.RECORD,
                    QuestProgressMode.EVENT_COUNT,
                    QuestProgressSourceType.DURABLE_OUTBOX_FACT,
                    "LifeLogRecorded",
                    "LIFE_LOG",
                    "event.playerId == questAcceptance.playerId; "
                            + "event.occurredAt >= acceptance.acceptedAt",
                    3,
                    QuestContentTargetUnit.DISTINCT_LIFELOG,
                    true,
                    QuestRepeatRule.ONCE,
                    QuestContentPeriodBoundary
                            .FROM_ACCEPTANCE_UNTIL_COMPLETION,
                    QuestContentTimezonePolicy.NOT_APPLICABLE,
                    null,
                    QuestRoleContextPolicy.OPTIONAL_SOURCE_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_EXP_AND_ITEM_FIRST_STEP_20",
                    false,
                    false,
                    null,
                    null,
                    null,
                    20,
                    "icon.quest.record.three_traces",
                    "quest.record.secondary",
                    "quest.q_record_three_traces.completed",
                    "quest.q_record_three_traces.empty",
                    List.of()
            )
    ),

    RECORD_WEEKLY_LOOKBACK(
            new SeedLevel1QuestDefinition(
                    QuestCode.Q_RECORD_WEEKLY_LOOKBACK,
                    1,
                    QuestContentPriority.P0,
                    1,
                    QuestDefinitionStatus.ACTIVE,
                    "이번 주 흔적 돌아보기",
                    "이번 주 기록 중 하나를 골라 지금의 나에게 남길 한 줄을 적어보세요.",
                    "별도 회고 전용 Entity를 만들지 않고 LifeLog subtype REFLECTION과 "
                            + "reflectionScope WEEKLY_LOOKBACK 메타데이터로 주간 회고를 "
                            + "식별합니다.",
                    QuestSemanticCategory.RECORD,
                    QuestProgressMode.EVENT_COUNT,
                    QuestProgressSourceType.DURABLE_OUTBOX_FACT,
                    "LifeLogRecorded",
                    "LIFE_LOG",
                    "event.playerId == questAcceptance.playerId; "
                            + "event.subtype == REFLECTION; "
                            + "event.reflectionScope == WEEKLY_LOOKBACK; "
                            + "event.periodKey == acceptance.periodKey",
                    1,
                    QuestContentTargetUnit.WEEKLY_REFLECTION,
                    true,
                    QuestRepeatRule.WEEKLY,
                    QuestContentPeriodBoundary
                            .MONDAY_00_00_INCLUSIVE_TO_NEXT_MONDAY_00_00_EXCLUSIVE,
                    QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE,
                    "Asia/Seoul",
                    QuestRoleContextPolicy.OPTIONAL_SOURCE_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_NONE",
                    false,
                    false,
                    null,
                    null,
                    null,
                    30,
                    "icon.quest.record.weekly_lookback",
                    "quest.record.reflection",
                    "quest.q_record_weekly_lookback.completed",
                    "quest.q_record_weekly_lookback.empty",
                    List.of()
            )
    ),

    GROWTH_ONE_FOCUS(
            new SeedLevel1QuestDefinition(
                    QuestCode.Q_GROWTH_ONE_FOCUS,
                    1,
                    QuestContentPriority.P0,
                    1,
                    QuestDefinitionStatus.ACTIVE,
                    "한 가지에 25분 집중하기",
                    "지금 가장 작은 한 가지를 골라 25분만 집중해보세요.",
                    "사용자는 시작 전에 집중할 한 가지를 1~80자 자유 텍스트로 입력하고, "
                            + "집중을 마친 뒤 직접 완료를 확인합니다. 서버는 집중 내용이나 "
                            + "성과를 평가하지 않습니다.",
                    QuestSemanticCategory.GROWTH,
                    QuestProgressMode.MANUAL_CHECK,
                    QuestProgressSourceType.USER_CONFIRMATION,
                    "NONE_DIRECT_COMMAND",
                    "QUEST_ACCEPTANCE",
                    "command.playerId == questAcceptance.playerId",
                    25,
                    QuestContentTargetUnit.MINUTE_INTENT,
                    false,
                    QuestRepeatRule.DAILY,
                    QuestContentPeriodBoundary
                            .LOCAL_DAY_00_00_TO_NEXT_DAY_00_00,
                    QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE,
                    "Asia/Seoul",
                    QuestRoleContextPolicy
                            .OPTIONAL_RECOMMENDED_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_NONE",
                    true,
                    false,
                    null,
                    null,
                    null,
                    40,
                    "icon.quest.growth.one_focus",
                    "quest.growth.primary",
                    "quest.q_growth_one_focus.completed",
                    "quest.q_growth_one_focus.empty",
                    List.of(
                            "Focus enum 신설 금지",
                            "focusLabel은 후속 Manual API에서 1~80자 필수",
                            "추천 Role은 optional이며 현재 single roleTemplateCode로 축약 금지"
                    )
            )
    ),

    RECOVERY_REST_TEN(
            new SeedLevel1QuestDefinition(
                    QuestCode.Q_RECOVERY_REST_TEN,
                    1,
                    QuestContentPriority.P0,
                    1,
                    QuestDefinitionStatus.ACTIVE,
                    "10분 쉬어가기",
                    "아무것도 증명하지 않아도 되는 10분을 가져보세요.",
                    "사용자가 10분 정도 쉬었다고 직접 확인하면 완료합니다. P0에서는 "
                            + "Timer, Exercise, LifeLog를 필수 원본으로 요구하지 않습니다.",
                    QuestSemanticCategory.RECOVERY,
                    QuestProgressMode.MANUAL_CHECK,
                    QuestProgressSourceType.USER_CONFIRMATION,
                    "NONE_DIRECT_COMMAND",
                    "QUEST_ACCEPTANCE",
                    "command.playerId == questAcceptance.playerId",
                    10,
                    QuestContentTargetUnit.MINUTE_INTENT,
                    false,
                    QuestRepeatRule.DAILY,
                    QuestContentPeriodBoundary
                            .LOCAL_DAY_00_00_TO_NEXT_DAY_00_00,
                    QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE,
                    "Asia/Seoul",
                    QuestRoleContextPolicy
                            .OPTIONAL_RECOMMENDED_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_NONE",
                    true,
                    false,
                    null,
                    "미완료 불이익/연속 수행 압박 금지",
                    null,
                    50,
                    "icon.quest.recovery.rest_ten",
                    "quest.recovery.calm",
                    "quest.q_recovery_rest_ten.completed",
                    "quest.q_recovery_rest_ten.empty",
                    List.of(
                            "Timer 증빙 강제 금지",
                            "부분 시간 누적 금지",
                            "미완료 불이익/연속 수행 압박 금지"
                    )
            )
    );

    private final SeedLevel1QuestDefinition definition;

    static {
        var codes = new HashSet<QuestCode>();
        var sortOrders = new HashSet<Integer>();
        for (SeedLevel1QuestDefinition definition : definitions()) {
            if (!codes.add(definition.questCode())) {
                throw new IllegalStateException(
                        "Seed Level 1 quest code must be unique"
                );
            }
            if (!sortOrders.add(definition.sortOrder())) {
                throw new IllegalStateException(
                        "Seed Level 1 quest sortOrder must be unique"
                );
            }
        }
    }

    SeedLevel1Quest(SeedLevel1QuestDefinition definition) {
        this.definition = definition;
    }

    public SeedLevel1QuestDefinition definition() {
        return definition;
    }

    public static List<SeedLevel1QuestDefinition> definitions() {
        return Arrays.stream(values())
                .map(SeedLevel1Quest::definition)
                .sorted(
                        java.util.Comparator.comparingInt(
                                SeedLevel1QuestDefinition::sortOrder
                        )
                )
                .toList();
    }
}
