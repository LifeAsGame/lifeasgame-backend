package online.lifeasgame.quest.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JpaQuestAcceptanceRepository extends JpaRepository<QuestAcceptance, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select acceptance from QuestAcceptance acceptance where acceptance.id = :acceptanceId")
    Optional<QuestAcceptance> findByIdForUpdate(
            @Param("acceptanceId") Long acceptanceId
    );

    List<QuestAcceptance> findAllByPlayerId(Long playerId);

    List<QuestAcceptance> findAllByPlayerIdAndStatus(Long playerId, QuestStatus status);

    List<QuestAcceptance> findAllByQuestId(Long questId);

    List<QuestAcceptance> findAllByQuestIdAndStatus(Long questId, QuestStatus status);

    Optional<QuestAcceptance> findTopByQuestIdAndPlayerIdOrderByIdDesc(Long questId, Long playerId);

    boolean existsByPlayerIdAndId(Long playerId, Long id);

    @Query("""
            SELECT DISTINCT acceptance.questId
            FROM QuestAcceptance acceptance
            WHERE acceptance.playerId = :playerId
              AND acceptance.status = :status
              AND acceptance.questId IN :questIds
            """)
    Set<Long> findQuestIdsByPlayerIdAndStatus(
            @Param("playerId") Long playerId,
            @Param("status") QuestStatus status,
            @Param("questIds") Set<Long> questIds
    );
}
