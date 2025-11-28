package online.lifeasgame.quest.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestStatus;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuestAcceptanceRepositoryAdapter implements QuestAcceptanceRepository {

    private final JpaQuestAcceptanceRepository jpaQuestAcceptanceRepository;

    @Override
    public QuestAcceptance save(QuestAcceptance acceptance) {
        return jpaQuestAcceptanceRepository.save(acceptance);
    }

    @Override
    public Optional<QuestAcceptance> findById(Long acceptanceId) {
        return jpaQuestAcceptanceRepository.findById(acceptanceId);
    }

    @Override
    public List<QuestAcceptance> findAllByPlayerId(Long playerId) {
        return jpaQuestAcceptanceRepository.findAllByPlayerId(playerId);
    }

    @Override
    public List<QuestAcceptance> findAllByPlayerIdAndStatus(Long playerId, QuestStatus status) {
        return jpaQuestAcceptanceRepository.findAllByPlayerIdAndStatus(playerId, status);
    }

    @Override
    public List<QuestAcceptance> findAllByQuestId(Long questId) {
        return jpaQuestAcceptanceRepository.findAllByQuestId(questId);
    }

    @Override
    public List<QuestAcceptance> findAllByQuestIdAndStatus(Long questId, QuestStatus status) {
        return jpaQuestAcceptanceRepository.findAllByQuestIdAndStatus(questId, status);
    }

    @Override
    public Optional<QuestAcceptance> findLatestByQuestAndPlayer(Long questId, Long playerId) {
        return jpaQuestAcceptanceRepository.findTopByQuestIdAndPlayerIdOrderByIdDesc(questId, playerId);
    }
}
