package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestStatus;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import online.lifeasgame.quest.domain.repository.QuestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class QuestReader {

    private final QuestRepository questRepository;
    private final QuestAcceptanceRepository questAcceptanceRepository;

    Quest getByCode(QuestCode code) {
        return questRepository.findByCode(code.value())
                .orElseThrow(() -> new IllegalArgumentException("Quest not found for code " + code.value()));
    }

    Quest getById(Long questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found for id " + questId));
    }

    Collection<Quest> getByIds(Collection<Long> questIds) {
        return questIds.stream().map(this::getById).toList();
    }

    List<Quest> findAll() {
        return List.copyOf(questRepository.findAll());
    }

    List<QuestAcceptance> findPlayerAcceptances(Long playerId, QuestStatus status) {
        if (status == null) {
            return questAcceptanceRepository.findAllByPlayerId(playerId);
        }
        return questAcceptanceRepository.findAllByPlayerIdAndStatus(playerId, status);
    }

    QuestAcceptance getAcceptance(Long acceptanceId) {
        return questAcceptanceRepository.findById(acceptanceId)
                .orElseThrow(() -> new IllegalArgumentException("Quest acceptance not found for id " + acceptanceId));
    }

    List<QuestAcceptance> findQuestAcceptances(Long questId, QuestStatus status) {
        if (status == null) {
            return questAcceptanceRepository.findAllByQuestId(questId);
        }
        return questAcceptanceRepository.findAllByQuestIdAndStatus(questId, status);
    }

    QuestAcceptance findLatest(Long questId, Long playerId) {
        return questAcceptanceRepository.findLatestByQuestAndPlayer(questId, playerId).orElse(null);
    }
}
