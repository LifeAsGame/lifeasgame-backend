package online.lifeasgame.reward.application.event;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.reward.application.QuestCompletionRewardService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestRewardReadyBridge {

    private final QuestCompletionRewardService rewardService;

    @EventListener
    public void onQuestRewardReady(QuestRewardReadyFact fact) {
        rewardService.process(fact);
    }
}
