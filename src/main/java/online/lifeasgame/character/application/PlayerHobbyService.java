package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerHobbyService {

    private final PlayerHobbyWriter playerHobbyWriter;
    private final PlayerHobbyReader playerHobbyReader;

    private final HobbyReader hobbyReader;
    private final PlayerReader playerReader;

    @Transactional
    public PlayerHobbyResult.Granted grantHobby(PlayerHobbyCommand.Grant command) {
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

        return PlayerHobbyResult.Granted.of(
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

    public List<PlayerHobbyResult.Info> getPlayerHobbyInfos(Long playerId) {
        List<PlayerHobbyView> playerHobbyViews = playerHobbyReader.getPlayerHobbyInfos(playerId);
        return playerHobbyViews.stream()
                .map(PlayerHobbyResult.Info::from)
                .toList();
    }

    @Transactional
    public PlayerHobbyResult.Created createPlayerHobby(
            Long playerId,
            PlayerHobbyCommand.Create command
    ) {
        PlayerHobby playerHobby = playerHobbyWriter.createPlayerHobby(
                PlayerHobby.create(
                        playerId,
                        command.hobbyId(),
                        command.name(),
                        command.detail(),
                        command.proficiency(),
                        PlayerHobbyStatus.parse(command.status()),
                        command.startedOn()
                )
        );

        return PlayerHobbyResult.Created.from(playerHobby);
    }

    @Transactional
    public PlayerHobbyResult.Changed changePlayerHobby(Long playerId, PlayerHobbyCommand.Change command) {
        PlayerHobby playerHobby = playerHobbyWriter.changePlayerHobby(
                playerId,
                command.hobbyId(),
                command.name(),
                command.detail(),
                command.proficiency(),
                PlayerHobbyStatus.parse(command.status()),
                command.startedOn()
        );

        return PlayerHobbyResult.Changed.from(playerHobby);
    }

    @Transactional
    public void deletePlayerHobby(Long playerId, Long hobbyId) {
        playerHobbyWriter.deletePlayerHobby(playerId, hobbyId);
    }
}
