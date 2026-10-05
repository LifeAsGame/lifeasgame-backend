package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.PlayerHobby;

import java.util.Optional;

public interface PlayerHobbyRepository {
    PlayerHobby save(PlayerHobby playerHobby);

    Optional<PlayerHobby> findByPlayerIdAndHobbyId(Long playerId, Long hobbyId);

    void deleteByPlayerIdAndHobbyId(Long playerId, Long hobbyId);

    boolean existsByPlayerIdAndHobbyId(Long playerId, Long hobbyId);

    Optional<PlayerHobby> findPrivate(Long playerId, Long ownedItemId);

    boolean existsPrivateName(Long playerId, String normalizedName);

    PlayerHobby saveAndFlush(PlayerHobby hobby);

    void delete(PlayerHobby hobby);

    java.util.List<PlayerHobby> findPrivateByPlayerId(Long playerId);
}
