package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.QuestRouteStep;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class QuestRouteCriteriaEvaluator {

    private final QuestAcceptanceRepository questAcceptanceRepository;

    public boolean isSatisfied(Long playerId, QuestRouteStep step) {
        Set<Long> requiredQuestIds = step.requiredQuestIds();
        if (requiredQuestIds.isEmpty()) return false;

        Set<Long> completedQuestIds =
                questAcceptanceRepository.findCompletedQuestIds(
                        playerId,
                        requiredQuestIds
                );
        return completedQuestIds.containsAll(requiredQuestIds);
    }
}
