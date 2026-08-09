package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import online.lifeasgame.quest.domain.repository.QuestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class QuestWriter {

    private final QuestRepository questRepository;
    private final QuestAcceptanceRepository questAcceptanceRepository;

    public Quest create(Quest quest) {
        return questRepository.save(quest);
    }

    public QuestAcceptance saveAcceptance(QuestAcceptance questAcceptance) {
        return questAcceptanceRepository.save(questAcceptance);
    }
}
