package online.lifeasgame.character.infra;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerHobbyQuery;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.repository.PlayerHobbyRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerHobbyRepositoryAdapter implements PlayerHobbyRepository, PlayerHobbyQuery {

    private final JpaPlayerHobbyRepository jpaRepository;

    @Override
    public PlayerHobby save(PlayerHobby playerHobby) {
        return jpaRepository.save(playerHobby);
    }

    @Override
    public Optional<PlayerHobby> findByPlayerIdAndHobbyId(Long playerId, Long hobbyId) {
        return jpaRepository.findByPlayerIdAndHobbyId(playerId, hobbyId);
    }

    @Override
    public void deleteByPlayerIdAndHobbyId(Long playerId, Long hobbyId) {
        jpaRepository.deleteByPlayerIdAndHobbyId(playerId, hobbyId);
    }

    @Override
    public boolean existsByPlayerIdAndHobbyId(Long playerId, Long hobbyId) {
        return jpaRepository.existsByPlayerIdAndHobbyId(playerId, hobbyId);
    }

    @Override
    public List<PlayerHobbyView> findViewsByPlayerId(Long playerId) {
        return jpaRepository.findPlayerHobbyViews(playerId);
    }

    @Override
    public Optional<PlayerHobby> findPrivate(Long playerId, Long ownedItemId) {
        return jpaRepository.findByIdAndPlayerIdAndSource(ownedItemId, playerId, "PRIVATE");
    }

    @Override
    public boolean existsPrivateName(Long playerId, String normalizedName) {
        return jpaRepository.existsByPlayerIdAndPrivateNameKey(playerId, normalizedName);
    }

    @Override
    public PlayerHobby saveAndFlush(PlayerHobby hobby) {
        return jpaRepository.saveAndFlush(hobby);
    }

    @Override
    public void delete(PlayerHobby hobby) {
        jpaRepository.delete(hobby);
    }

    @Override
    public List<PlayerHobby> findPrivateByPlayerId(Long playerId) {
        return jpaRepository.findByPlayerIdAndSourceOrderByIdDesc(playerId, "PRIVATE");
    }
}
