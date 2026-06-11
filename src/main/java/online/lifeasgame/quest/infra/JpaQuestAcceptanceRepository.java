package online.lifeasgame.quest.infra;

import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaQuestAcceptanceRepository extends JpaRepository<QuestAcceptance, Long> {
    List<QuestAcceptance> findAllByPlayerId(Long playerId);

    List<QuestAcceptance> findAllByPlayerIdAndStatus(Long playerId, QuestStatus status);

    List<QuestAcceptance> findAllByQuestId(Long questId);

    List<QuestAcceptance> findAllByQuestIdAndStatus(Long questId, QuestStatus status);

    Optional<QuestAcceptance> findTopByQuestIdAndPlayerIdOrderByIdDesc(Long questId, Long playerId);

    boolean existsByPlayerIdAndId(Long playerId, Long id);

    void deleteByPlayerIdAndId(Long playerId, Long id);
}
