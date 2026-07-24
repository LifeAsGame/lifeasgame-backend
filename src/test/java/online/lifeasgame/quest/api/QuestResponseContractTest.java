package online.lifeasgame.quest.api;

import online.lifeasgame.quest.api.admin.mapper.AdminQuestWebMapper;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import online.lifeasgame.quest.api.player.mapper.QuestWebMapper;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Quest Acceptance 응답 계약")
class QuestResponseContractTest {

    @Nested
    @DisplayName("완료된 Acceptance를 응답으로 변환할 때")
    class MapCompletedAcceptance {

        @Test
        @DisplayName("Player와 Admin 응답은 COMPLETED와 새 상태 필드를 반환하고 DONE을 노출하지 않는다")
        void exposesCompletedContract() {
            Instant reachedAt = Instant.parse("2026-07-23T03:00:00Z");
            Instant completedAt = reachedAt.plusSeconds(60);
            Quest quest = quest();
            QuestAcceptance acceptance = QuestAcceptance.start(
                    quest.getId(),
                    1930L,
                    TimePeriod.forever()
            );
            ReflectionTestUtils.setField(acceptance, "id", 19300L);
            acceptance.setProgress(1, quest, reachedAt);
            acceptance.complete(completedAt);
            QuestResult.Acceptance result = QuestResult.Acceptance.from(acceptance, quest);

            QuestResponse.Acceptance playerResponse =
                    QuestWebMapper.toAcceptance(result);
            AdminQuestResponse.Acceptance adminResponse =
                    AdminQuestWebMapper.toAcceptance(result);

            assertThat(playerResponse.status()).isEqualTo(QuestStatus.COMPLETED.name());
            assertThat(playerResponse.status()).isNotEqualTo("DONE");
            assertThat(playerResponse.progressValue()).isEqualTo(1);
            assertThat(playerResponse.completionPolicy())
                    .isEqualTo(QuestCompletionPolicy.USER_CONFIRM.name());
            assertThat(playerResponse.goalReachedAt()).isEqualTo(reachedAt);
            assertThat(playerResponse.completedAt()).isEqualTo(completedAt);

            assertThat(adminResponse.status()).isEqualTo(QuestStatus.COMPLETED.name());
            assertThat(adminResponse.status()).isNotEqualTo("DONE");
            assertThat(adminResponse.progressValue()).isEqualTo(1);
            assertThat(adminResponse.completionPolicy())
                    .isEqualTo(QuestCompletionPolicy.USER_CONFIRM.name());
            assertThat(adminResponse.goalReachedAt()).isEqualTo(reachedAt);
            assertThat(adminResponse.completedAt()).isEqualTo(completedAt);
        }
    }

    private Quest quest() {
        Quest quest = Quest.create(
                "quest:test:response-contract",
                QuestCategory.MAIN,
                QuestTitle.of("응답 계약 테스트"),
                "Quest 응답 계약 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.USER_CONFIRM,
                null
        );
        ReflectionTestUtils.setField(quest, "id", 193L);
        return quest;
    }
}
