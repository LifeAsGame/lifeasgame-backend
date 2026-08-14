package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
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
class PlayerHobbyUpdater {

    private final PlayerHobbyRepository repository;

    public PlayerHobby update(
            Long playerId,
            PlayerHobbyCommand.Change command
    ) {
        PlayerHobby playerHobby = repository.findByPlayerIdAndHobbyId(
                        playerId,
                        command.hobbyId()
                )
                .orElseThrow(() -> new DomainException(
                        PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND
                ));

        playerHobby.changeHobby(
                command.name() != null
                        ? command.name()
                        : playerHobby.getCustomName(),
                command.detail() != null
                        ? command.detail()
                        : playerHobby.getDetail(),
                command.proficiency() != null
                        ? command.proficiency()
                        : playerHobby.getProficiency(),
                command.status() != null
                        ? PlayerHobbyStatus.parse(command.status())
                        : playerHobby.getStatus(),
                command.startedOn() != null
                        ? command.startedOn()
                        : playerHobby.getStartedOn()
        );

        return playerHobby;
    }
}
