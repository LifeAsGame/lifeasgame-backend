package online.lifeasgame.reward.application.event;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.reward.application.QuestCompletionRewardService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestRewardReadyBridge {

    private final QuestCompletionRewardService rewardService;

    @EventListener
    public void onQuestEvent(QuestEvent event) {
        if (event.type() != QuestEventType.QUEST_REWARD_READY) {
            return;
        }
        QuestRewardReadyFact.from(event).ifPresent(rewardService::process);
    }
}
