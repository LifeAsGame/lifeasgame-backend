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
                    "허용 subtype의 사용자 작성 LifeLogRecorded 1건을 수신하면 goal reached와 "
                            + "QuestCompleted를 한 번만 확정한다. 동일 eventId 또는 "
                            + "lifeLogId 재전달은 무시한다.",
                    true,
                    QuestRepeatRule.ONCE,
                    QuestContentPeriodBoundary.PLAYER_LIFETIME,
                    QuestContentTimezonePolicy.NOT_APPLICABLE,
                    null,
                    null,
                    null,
                    QuestRoleContextPolicy.OPTIONAL_SOURCE_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_EXP_TINY_10",
                    false,
                    false,
                    "완료 전 취소 가능; 재수락 시 새 attempt로 시작하며 새 acceptedAt 이후 "
                            + "이벤트만 인정; 한 번 완료되면 재수락 불가",
                    "미완료·취소에 페널티, 연속 수행 손실, 죄책감 문구 없음",
                    "Q_RECORD_THREE_TRACES 또는 방금 남긴 LifeLog 보기",
                    10,
                    "icon.quest.record.first_trace",
                    "quest.record.primary",
                    "quest.q_record_first_trace.completed",
                    "quest.q_record_first_trace.empty",
                    "허용 subtype: QUICK_NOTE|ACTIVITY|STUDY|PROJECT|MEMORY|REFLECTION|"
                            + "MOOD|HEALTH_NOTE. SYSTEM_GENERATED는 제외. 기록 생성 후 "
                            + "삭제되어도 이미 확정된 완료 사실과 보상은 되돌리지 않는다. "
                            + "삭제 사실은 별도 정책이며 Quest 진행도 감소 원인이 아니다."
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
                    "서로 다른 eventId이면서 서로 다른 lifeLogId인 LifeLogRecorded 세 건만 "
                            + "누적한다. 세 번째 고유 기록에서 goal reached와 "
                            + "QuestCompleted를 한 번만 확정한다.",
                    true,
                    QuestRepeatRule.ONCE,
                    QuestContentPeriodBoundary
                            .FROM_ACCEPTANCE_UNTIL_COMPLETION,
                    QuestContentTimezonePolicy.NOT_APPLICABLE,
                    null,
                    null,
                    null,
                    QuestRoleContextPolicy.OPTIONAL_SOURCE_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_EXP_AND_ITEM_FIRST_STEP_20",
                    false,
                    false,
                    "완료 전 취소 가능; 재수락 시 진행도 0으로 새 attempt 시작; 이전 "
                            + "attempt의 기록 이벤트는 새 attempt에 이월하지 않음",
                    "기간 만료와 연속 수행 압박 없음; 중단해도 실패 낙인 없음",
                    "Mailbox에서 첫걸음의 조각 확인 또는 ROUTE_RECORD_START 다음 단계 확인",
                    20,
                    "icon.quest.record.three_traces",
                    "quest.record.secondary",
                    "quest.q_record_three_traces.completed",
                    "quest.q_record_three_traces.empty",
                    "수행 기간 제한 없음. 동일 LifeLog 수정 이벤트는 진행도 증가 없음. "
                            + "동일 이벤트 재전달은 eventId 멱등 처리. 기록 삭제 후에도 "
                            + "이미 누적·완료된 사실은 유지한다."
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
                    "해당 주기 안에 생성된 REFLECTION LifeLog 중 "
                            + "reflectionScope=WEEKLY_LOOKBACK인 고유 기록 1건을 수신하면 "
                            + "자동 완료한다. 본문 내용은 판정하지 않는다.",
                    true,
                    QuestRepeatRule.WEEKLY,
                    QuestContentPeriodBoundary
                            .MONDAY_00_00_INCLUSIVE_TO_NEXT_MONDAY_00_00_EXCLUSIVE,
                    QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE,
                    "Asia/Seoul",
                    null,
                    null,
                    QuestRoleContextPolicy.OPTIONAL_SOURCE_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_NONE",
                    false,
                    false,
                    "해당 주기 내 완료 전 취소 가능; 다음 주기에 새 acceptance 생성 가능; "
                            + "이전 주기 미완료는 EXPIRED가 아니라 CLOSED_INCOMPLETE로 "
                            + "기록하되 사용자에게 실패로 표현하지 않음",
                    "미완료 페널티·연속 주차 손실·재촉 알림 없음",
                    "ROUTE_RECORD_START의 준비된 마지막 Step을 직접 advance하거나 "
                            + "회고 LifeLog 보기",
                    30,
                    "icon.quest.record.weekly_lookback",
                    "quest.record.reflection",
                    "quest.q_record_weekly_lookback.completed",
                    "quest.q_record_weekly_lookback.empty",
                    "회고 전용 subtype 신설 불필요. REFLECTION + reflectionScope로 구분. "
                            + "Quick LifeLog는 인정하지 않음. 다음 주기 재수행 가능. "
                            + "주기당 Player별 완료 1회."
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
                    "focusLabel 필수, memo 선택. 사용자가 약 25분의 집중을 마쳤다고 확인하면 "
                            + "완료한다. 타이머 증빙·성과 검증·텍스트 분석은 하지 않는다.",
                    false,
                    QuestRepeatRule.DAILY,
                    QuestContentPeriodBoundary
                            .LOCAL_DAY_00_00_TO_NEXT_DAY_00_00,
                    QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE,
                    "Asia/Seoul",
                    null,
                    null,
                    QuestRoleContextPolicy
                            .OPTIONAL_RECOMMENDED_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_NONE",
                    true,
                    false,
                    "당일 완료 전 취소 가능; 같은 일자 재수락 시 새 attempt로 시작; "
                            + "완료 후 같은 periodKey 재완료 불가",
                    "미완료·짧게 끝냄·성과 없음에 페널티나 평가 문구 없음",
                    "집중 중 알게 된 점을 짧은 LifeLog로 남기거나 오늘은 여기서 마치기",
                    40,
                    "icon.quest.growth.one_focus",
                    "quest.growth.primary",
                    "quest.q_growth_one_focus.completed",
                    "quest.q_growth_one_focus.empty",
                    "Focus 유형 domain enum은 P0에 만들지 않는다. 선택형 preset은 FE "
                            + "제안값일 뿐 저장·판정 필수값이 아니다. 필수 입력은 "
                            + "focusLabel 하나. 추천 Role Template: "
                            + "ROLE_JOB_SEEKER|ROLE_BACKEND_DEVELOPER."
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
                    "한 번의 휴식 세션을 사용자가 직접 확인하면 완료한다. 부분 수행 시간은 "
                            + "누적하지 않으며 메모와 휴식 이유는 요구하지 않는다.",
                    false,
                    QuestRepeatRule.DAILY,
                    QuestContentPeriodBoundary
                            .LOCAL_DAY_00_00_TO_NEXT_DAY_00_00,
                    QuestContentTimezonePolicy.PLAYER_PROFILE_TIMEZONE,
                    "Asia/Seoul",
                    null,
                    null,
                    QuestRoleContextPolicy
                            .OPTIONAL_RECOMMENDED_ROLE_CONTEXT,
                    Set.of("ANY"),
                    "RP_NONE",
                    true,
                    false,
                    "언제든 완료 전 취소 가능; 같은 일자 재수락 가능; 미완료 attempt는 "
                            + "일자 경계에서 CLOSED_INCOMPLETE 처리",
                    "실패·연속 수행·손실·마감 압박 없음. 미완료 상태에서도 휴식을 권리처럼 "
                            + "표현하며 재촉하지 않음",
                    "조금 더 쉬거나, 상태가 괜찮다면 짧은 LifeLog를 남기기",
                    50,
                    "icon.quest.recovery.rest_ten",
                    "quest.recovery.calm",
                    "quest.q_recovery_rest_ten.completed",
                    "quest.q_recovery_rest_ten.empty",
                    "10의 단위는 minute. P0 인정 원본은 수동 확인. Rest Timer는 P1 "
                            + "보조 입력 후보이며 완료 증빙으로 강제하지 않는다. "
                            + "Exercise와 일반 LifeLog는 이 Quest의 완료 원본으로 "
                            + "사용하지 않는다."
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
