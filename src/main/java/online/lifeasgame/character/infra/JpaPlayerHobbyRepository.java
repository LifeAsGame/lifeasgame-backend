package online.lifeasgame.character.infra;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.PlayerHobby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPlayerHobbyRepository extends JpaRepository<PlayerHobby, Long> {

    @Query(
        """
            SELECT h.id AS hobbyId,
                   h.name AS name,
                   h.category  AS category,
                   ph.customName AS customName,
                   ph.detail AS detail,
                   ph.proficiency AS proficiency,
                   ph.status AS status,
                   ph.startedOn AS startedOn,
                   ph.xp AS xp,
                   ph.personalCategoryId AS personalCategoryId
            FROM PlayerHobby ph
            JOIN Hobby h ON h.id = ph.hobbyId
            WHERE ph.playerId = :playerId
            ORDER BY ph.startedOn DESC
        """
    )
    List<PlayerHobbyView> findPlayerHobbyViews(@Param("playerId") Long playerId);

    Optional<PlayerHobby> findByPlayerIdAndHobbyId(Long playerId, Long HobbyId);

    void deleteByPlayerIdAndHobbyId(Long playerId, Long HobbyId);

    boolean existsByPlayerIdAndHobbyId(Long playerId, Long HobbyId);

    Optional<PlayerHobby> findByIdAndPlayerIdAndSource(Long id, Long playerId, String source);

    boolean existsByPlayerIdAndPrivateNameKey(Long playerId, String privateNameKey);

    List<PlayerHobby> findByPlayerIdAndSourceOrderByIdDesc(Long playerId, String source);

    @Query("select distinct h.category from PlayerHobby ph join Hobby h on h.id = ph.hobbyId where ph.playerId = :playerId")
    List<online.lifeasgame.character.domain.HobbyCategory> ownedCategories(@Param("playerId") Long playerId);
}
