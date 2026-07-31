package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestStatus;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import online.lifeasgame.quest.domain.repository.QuestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class QuestReader {

    private final QuestRepository questRepository;
    private final QuestAcceptanceRepository questAcceptanceRepository;

    Optional<Quest> findByCode(QuestCode code) {
        return questRepository.findByCode(code.value());
    }

    Quest getByCode(QuestCode code) {
        return questRepository.findByCode(code.value())
                .orElseThrow(() -> new DomainException(QuestError.QUEST_NOT_FOUND));
    }

    Quest getById(Long questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> new DomainException(QuestError.QUEST_NOT_FOUND));
    }

    Collection<Quest> getByIds(Collection<Long> questIds) {
        return questRepository.findAllByIds(questIds);
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
                .orElseThrow(() -> new DomainException(QuestError.QUEST_ACCEPTANCE_NOT_FOUND));
    }

    QuestAcceptance getAcceptanceForUpdate(Long acceptanceId) {
        return questAcceptanceRepository.findByIdForUpdate(acceptanceId)
                .orElseThrow(() -> new DomainException(QuestError.QUEST_ACCEPTANCE_NOT_FOUND));
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

    public void assertAcceptanceIsExists(Long playerId, Long questId) {
        if (questAcceptanceRepository.existsByPlayerIdAndId(playerId, questId)) {
            throw new DomainException(QuestError.QUEST_ACCEPTANCE_ALREADY_EXISTS);
        }
    }
}
