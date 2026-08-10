package online.lifeasgame.quest.domain.repository;

import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QuestAcceptanceRepository {
    QuestAcceptance save(QuestAcceptance acceptance);

    Optional<QuestAcceptance> findById(Long acceptanceId);

    Optional<QuestAcceptance> findByIdForUpdate(Long acceptanceId);

    List<QuestAcceptance> findAllByPlayerId(Long playerId);

    List<QuestAcceptance> findAllByPlayerIdAndStatus(Long playerId, QuestStatus status);

    List<QuestAcceptance> findAllByQuestId(Long questId);

    List<QuestAcceptance> findAllByQuestIdAndStatus(Long questId, QuestStatus status);

    Optional<QuestAcceptance> findLatestByQuestAndPlayer(Long questId, Long playerId);

    boolean existsByPlayerIdAndId(Long playerId, Long questId);

    Set<Long> findCompletedQuestIds(Long playerId, Set<Long> questIds);
}
