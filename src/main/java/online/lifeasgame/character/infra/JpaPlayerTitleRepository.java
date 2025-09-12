package online.lifeasgame.character.infra;

import online.lifeasgame.character.domain.PlayerTitle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlayerTitleRepository extends JpaRepository<PlayerTitle, Long> {

    boolean existsByPlayerIdAndTitleId(Long playerId, Long titleId);
}
