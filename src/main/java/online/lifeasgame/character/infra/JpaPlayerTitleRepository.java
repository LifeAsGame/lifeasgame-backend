package online.lifeasgame.character.infra;

import java.util.List;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.PlayerTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPlayerTitleRepository extends JpaRepository<PlayerTitle, Long> {

    @Query(
        """
            SELECT t.id AS titleId,
                   t.code AS code,
                   t.name AS name,
                   t.category  AS category,
                   t.descMd          AS descMd,
                   pt.acquiredAt     AS acquiredAt
            FROM PlayerTitle pt
            JOIN Title t ON t.id = pt.titleId
            WHERE pt.playerId = :playerId
            ORDER BY pt.acquiredAt DESC
    """)
    List<PlayerTitleView> findPlayerTitleViews(@Param("playerId") Long playerId);

    boolean existsByPlayerIdAndTitleId(Long playerId, Long titleId);
}
