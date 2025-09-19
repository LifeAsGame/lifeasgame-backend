package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AdminPlayerHobbyCommand;
import online.lifeasgame.character.application.result.AdminPlayerHobbyResult;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class AdminPlayerHobbyService {

    private final HobbyReader hobbyReader;
    private final PlayerHobbyWriter playerHobbyWriter;
    private final PlayerReader playerReader;

    @Transactional
    public AdminPlayerHobbyResult.GrantedHobby grantHobby(AdminPlayerHobbyCommand.GrantHobby command) {
        if (playerReader.notExists(command.playerId())) {
            throw new DomainException(PlayerError.PLAYER_NOT_FOUND);
        }

        Hobby hobby = hobbyReader.getHobby(command.hobbyId());

        PlayerHobby saved = playerHobbyWriter.grantHobby(
                PlayerHobby.create(
                        command.playerId(),
                        command.hobbyId(),
                        command.customName(),
                        command.detail(),
                        command.proficiency(),
                        PlayerHobbyStatus.parse(command.status()),
                        command.startedOn()
                )
        );

        return AdminPlayerHobbyResult.GrantedHobby.of(
                saved.getPlayerId(),
                saved.getHobbyId(),
                hobby.getName(),
                hobby.getCategory().name(),
                saved.getCustomName(),
                saved.getDetail(),
                saved.getProficiency(),
                saved.getStatus().name(),
                saved.getStartedOn(),
                saved.getXp()
        );
    }
}
