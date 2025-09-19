package online.lifeasgame.character.application;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import online.lifeasgame.character.domain.repository.PlayerHobbyRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PlayerHobbyWriter {

    private final PlayerHobbyRepository repository;

    public PlayerHobby grantHobby(PlayerHobby playerHobby) {
        return repository.save(playerHobby);
    }

    public PlayerHobby createPlayerHobby(PlayerHobby playerHobby) {
        return repository.save(playerHobby);
    }

    public PlayerHobby changePlayerHobby(
            Long playerId,
            Long hobbyId,
            String name,
            String detail,
            Integer proficiency,
            PlayerHobbyStatus status,
            LocalDate startedOn
    ) {
        PlayerHobby playerHobby  = repository.findByPlayerIdAndHobbyId(playerId, hobbyId)
                .orElseThrow(() -> new DomainException(PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND));

        playerHobby.changeHobby(
                name,
                detail,
                proficiency,
                status,
                startedOn
        );

        return playerHobby;
    }

    public void deletePlayerHobby(Long playerId, Long hobbyId) {
        if (!repository.existsByPlayerIdAndHobbyId(playerId, hobbyId)) {
            throw new DomainException(PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND);
        }

        repository.deleteByPlayerIdAndHobbyId(playerId, hobbyId);
    }
}
