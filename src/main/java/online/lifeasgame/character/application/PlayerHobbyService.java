package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
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
    public PlayerHobbyResult.Created createPlayerHobby(Long playerId, PlayerHobbyCommand.Create command) {
        playerReader.assertExistsById(playerId);

        Hobby hobby = hobbyReader.getByIdOrThrow(command.hobbyId());

        PlayerHobby playerHobby = playerHobbyWriter.create(
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

        return PlayerHobbyResult.Created.from(playerHobby, hobby);
    }

    public List<PlayerHobbyResult.Info> getPlayerHobbyInfos(Long playerId) {
        List<PlayerHobbyView> playerHobbyViews = playerHobbyReader.getViewsByPlayerId(playerId);
        return playerHobbyViews.stream()
                .map(PlayerHobbyResult.Info::from)
                .toList();
    }

    @Transactional
    public PlayerHobbyResult.Changed changePlayerHobby(Long playerId, PlayerHobbyCommand.Change command) {
        PlayerHobby playerHobby = playerHobbyReader.getByPlayerIdAndHobbyId(playerId, command.hobbyId());

        playerHobby.changeHobby(
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
        playerHobbyWriter.deleteByPlayerIdAndHobbyId(playerId, hobbyId);
    }
}
