package online.lifeasgame.character.infra;

import java.util.List;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.domain.PlayerAchievement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaPlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {

    @Query(
            """
                SELECT t.id AS achievementId,
                       t.code AS code,
                       t.name AS name,
                       t.category  AS category,
                       t.descMd          AS descMd,
                       pa.acquiredAt     AS acquiredAt
                FROM PlayerAchievement pa
                JOIN Achievement t ON t.id = pa.achievementId
                WHERE pa.playerId = :playerId
                ORDER BY pa.acquiredAt DESC, pa.id DESC
        """)
    List<PlayerAchievementView> findPlayerAchievementViews(Long playerId);

    @Query(
            """
                SELECT t.id AS achievementId,
                       t.code AS code,
                       t.name AS name,
                       t.category AS category,
                       t.descMd AS descMd,
                       pa.acquiredAt AS acquiredAt
                FROM PlayerAchievement pa
                JOIN Achievement t ON t.id = pa.achievementId
                WHERE pa.playerId = :playerId
                ORDER BY pa.acquiredAt DESC, pa.id DESC
        """)
    List<PlayerAchievementView> findRecentPlayerAchievementViews(
            Long playerId,
            Pageable pageable
    );

    void deleteByPlayerIdAndAchievementId(Long playerId, Long achievementId);
}
