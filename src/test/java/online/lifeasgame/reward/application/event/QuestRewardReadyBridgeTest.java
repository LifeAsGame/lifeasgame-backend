package online.lifeasgame.reward.application.event;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.reward.application.QuestCompletionRewardService;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@DisplayName("QuestRewardReady Bridge/Fact")
class QuestRewardReadyBridgeTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-30T03:00:01Z");

    @Test
    @DisplayName("final contract를 acceptanceId Settlement fact로 전달한다")
    void translatesFinalContract() {
        QuestCompletionRewardService service =
                mock(QuestCompletionRewardService.class);
        QuestRewardReadyBridge bridge = new QuestRewardReadyBridge(service);

        bridge.onQuestEvent(event(Map.of(
                "acceptanceId", 21900L,
                "rewardProfileCode", "RP_EXP_TINY_10",
                "questDefinitionVersion", 7
        )));

        var captor = forClass(QuestRewardReadyFact.class);
        verify(service).process(captor.capture());
        QuestRewardReadyFact fact = captor.getValue();
        assertThat(fact.playerId()).isEqualTo(2190L);
        assertThat(fact.acceptanceId()).isEqualTo(21900L);
        assertThat(fact.rewardProfileCode())
                .isEqualTo("RP_EXP_TINY_10");
        assertThat(fact.questId()).isEqualTo(219L);
        assertThat(fact.questCode()).isEqualTo("Q_FIRST_STEP");
        assertThat(fact.questDefinitionVersion()).isEqualTo(7);
        assertThat(fact.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(fact.correlationId())
                .isEqualTo("quest:219:completed:reward");
    }

    @Test
    @DisplayName("legacy Event는 no-op이고 final marker가 있는 profile 누락은 stable error다")
    void gatesLegacyAndMalformedFinalEvents() {
        QuestCompletionRewardService service =
                mock(QuestCompletionRewardService.class);
        QuestRewardReadyBridge bridge = new QuestRewardReadyBridge(service);

        bridge.onQuestEvent(event(Map.of("acceptanceId", 21900L)));
        verifyNoInteractions(service);

        assertRewardError(
                () -> bridge.onQuestEvent(event(Map.of(
                        "acceptanceId", 21900L,
                        "progressSource", "COUNT"
                ))),
                RewardError.REWARD_PROFILE_CODE_REQUIRED
        );
    }

    @Test
    @DisplayName("acceptanceId는 integral exact positive long만 허용한다")
    void validatesAcceptanceIdentityExactly() {
        assertThat(QuestRewardReadyFact.from(event(Map.of(
                "acceptanceId", BigInteger.valueOf(21900L),
                "rewardProfileCode", "RP_NONE"
        ))).orElseThrow().acceptanceId()).isEqualTo(21900L);
        assertThat(QuestRewardReadyFact.from(event(Map.of(
                "acceptanceId", new BigDecimal("21900.0"),
                "rewardProfileCode", "RP_NONE"
        ))).orElseThrow().acceptanceId()).isEqualTo(21900L);

        for (Object invalid : new Object[]{
                "21900",
                21900.5,
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE),
                0L
        }) {
            assertRewardError(
                    () -> QuestRewardReadyFact.from(event(Map.of(
                            "acceptanceId", invalid,
                            "rewardProfileCode", "RP_NONE"
                    ))),
                    RewardError.REWARD_SETTLEMENT_SOURCE_ID_REQUIRED
            );
        }
    }

    @Test
    @DisplayName("invalid player/profile과 다른 Quest Event는 안전하게 처리한다")
    void validatesPlayerAndProfileAndIgnoresOtherTypes() {
        assertRewardError(
                () -> QuestRewardReadyFact.from(new QuestEvent(
                        QuestEventType.QUEST_REWARD_READY,
                        0L,
                        219L,
                        "Q_FIRST_STEP",
                        Map.of(
                                "acceptanceId", 21900L,
                                "rewardProfileCode", "RP_NONE"
                        ),
                        OCCURRED_AT,
                        "reward"
                )),
                RewardError.REWARD_SETTLEMENT_PLAYER_ID_REQUIRED
        );
        assertRewardError(
                () -> QuestRewardReadyFact.from(event(Map.of(
                        "acceptanceId", 21900L,
                        "rewardProfileCode", " "
                ))),
                RewardError.REWARD_PROFILE_CODE_REQUIRED
        );

        QuestCompletionRewardService service =
                mock(QuestCompletionRewardService.class);
        new QuestRewardReadyBridge(service).onQuestEvent(new QuestEvent(
                QuestEventType.QUEST_COMPLETED,
                2190L,
                219L,
                "Q_FIRST_STEP",
                Map.of(),
                OCCURRED_AT,
                "completed"
        ));
        verifyNoInteractions(service);
    }

    private QuestEvent event(Map<String, Object> attributes) {
        return new QuestEvent(
                QuestEventType.QUEST_REWARD_READY,
                2190L,
                219L,
                "Q_FIRST_STEP",
                attributes,
                OCCURRED_AT,
                "quest:219:completed:reward"
        );
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
