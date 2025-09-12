package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.PlayerTitle;

public interface PlayerTitleRepository {
    PlayerTitle save(PlayerTitle playerTitle);

    boolean existsByPlayerIdAndTitleId(Long playerId, Long titleId);
}
