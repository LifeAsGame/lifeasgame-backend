package online.lifeasgame.character.application.internal;

public interface PlayerExistenceReadApi {

    boolean existsByPlayerId(Long playerId);
}
