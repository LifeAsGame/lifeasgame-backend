package online.lifeasgame.reward.application.event;

import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.reward.application.QuestCompletionRewardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("QuestRewardReadyBridge")
class QuestRewardReadyBridgeTest {

    @Test
    @DisplayName("Quest provider-owned typed fact를 그대로 Reward Service에 전달한다")
    void forwardsProviderOwnedFact() {
        QuestCompletionRewardService service =
                mock(QuestCompletionRewardService.class);
        QuestRewardReadyBridge bridge = new QuestRewardReadyBridge(service);
        QuestRewardReadyFact fact = new QuestRewardReadyFact(
                QuestRewardReadyFact.EVENT_VERSION,
                2190L,
                21900L,
                "RP_EXP_TINY_10",
                219L,
                "Q_FIRST_STEP",
                7,
                Instant.parse("2026-07-30T03:00:01Z"),
                "quest:219:completed:reward"
        );

        bridge.onQuestRewardReady(fact);

        verify(service).process(fact);
    }
}
