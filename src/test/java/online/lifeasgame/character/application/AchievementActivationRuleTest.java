package online.lifeasgame.character.application;

import java.time.Instant;
import online.lifeasgame.inventory.domain.event.ItemRewardClaimed;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.domain.event.QuestRouteCompleted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("업적 활성화 조건")
class AchievementActivationRuleTest {
    private static final Instant TIME = Instant.parse("2026-10-06T00:00:00Z");

    @Test
    @DisplayName("내용이 완성된 LifeLog는 퀘스트 수락과 관계없이 첫 기록 근거가 된다")
    void contentReadyLifeLog() {
        var fact = new LifeLogRecorded("event-1", LifeLogRecorded.EVENT_TYPE, 1,
                TIME, 7L, 10L, 1, LifeLogSubtype.STUDY,
                LifeLogEntryMode.FULL, null, null, null, null);
        var result = AchievementActivationRule.match(fact).orElseThrow();
        assertThat(result.achievementCode()).isEqualTo("ACH_FIRST_LIFELOG");
        assertThat(result.linkedTitleCode()).isEqualTo("TITLE_CANDIDATE_RECORD_BEGINNER");
        assertThat(AchievementActivationRule.match(LifeLogRecorded.legacy(
                "event-2", 1, 7L, 11L, LifeLogType.COLLECTION, null, TIME)))
                .isEmpty();
    }

    @Test
    @DisplayName("완료 확정만 퀘스트 업적 근거가 된다")
    void completedQuestOnly() {
        var goal = QuestEvent.builder(QuestEventType.QUEST_GOAL_REACHED)
                .playerId(7L).attribute("acceptanceId", 20L)
                .occurredAt(TIME).build();
        var completed = QuestEvent.builder(QuestEventType.QUEST_COMPLETED)
                .playerId(7L).attribute("acceptanceId", 20L)
                .attribute("completedAt", TIME).occurredAt(TIME).build();
        assertThat(AchievementActivationRule.match(goal)).isEmpty();
        assertThat(AchievementActivationRule.match(completed).orElseThrow()
                .achievementCode()).isEqualTo("ACH_FIRST_QUEST_COMPLETE");
    }

    @Test
    @DisplayName("실제 Mailbox Claim만 첫 아이템 수령 근거가 된다")
    void committedClaimOnly() {
        assertThat(AchievementActivationRule.match(
                new ItemRewardClaimed(7L, 30L, 40L, 1, TIME)).orElseThrow()
                .achievementCode()).isEqualTo("ACH_FIRST_ITEM_CLAIM");
    }

    @Test
    @DisplayName("명시적으로 완료된 두 여정에만 각각의 업적과 연결 칭호를 배정한다")
    void completedRoutesOnly() {
        var record = AchievementActivationRule.match(new QuestRouteCompleted(
                7L, 50L, "ROUTE_RECORD_START", TIME)).orElseThrow();
        var backend = AchievementActivationRule.match(new QuestRouteCompleted(
                7L, 51L, "ROUTE_BACKEND_DEVELOPER_START", TIME)).orElseThrow();
        assertThat(record.achievementCode()).isEqualTo("ACH_ROUTE_RECORD_START");
        assertThat(record.linkedTitleCode()).isNull();
        assertThat(backend.achievementCode()).isEqualTo("ACH_ROUTE_BACKEND_START");
        assertThat(backend.linkedTitleCode()).isEqualTo("TITLE_BACKEND_GUIDE");
        assertThat(AchievementActivationRule.match(new QuestRouteCompleted(
                7L, 52L, "OTHER", TIME))).isEmpty();
    }
}
