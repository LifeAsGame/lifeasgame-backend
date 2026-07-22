package online.lifeasgame.character.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.character.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaPlayerRepository extends JpaRepository<Player, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select player from Player player where player.id = :playerId")
    Optional<Player> findByIdForUpdate(@Param("playerId") Long playerId);

    boolean existsByUserId(Long userId);

    Optional<Player> findByUserId(Long userId);
}
