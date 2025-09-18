package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerHobbyService {

    private final PlayerHobbyWriter playerHobbyWriter;
    private final PlayerHobbyReader playerHobbyReader;

    public List<PlayerHobbyResult.PlayerHobbyInfo> getPlayerHobbyInfos(Long playerId) {
        List<PlayerHobbyView> playerHobbyViews = playerHobbyReader.getPlayerHobbyInfos(playerId);
        return playerHobbyViews.stream()
                .map(PlayerHobbyResult.PlayerHobbyInfo::from)
                .toList();
    }

    @Transactional
    public PlayerHobbyResult.CreatedPlayerHobby createPlayerHobby(
            Long playerId,
            PlayerHobbyCommand.CreatePlayerHobby command
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

        return PlayerHobbyResult.CreatedPlayerHobby.from(playerHobby);
    }

    @Transactional
    public PlayerHobbyResult.ChangedPlayerHobby changePlayerHobby(Long playerId, PlayerHobbyCommand.ChangePlayerHobby command) {
        PlayerHobby playerHobby = playerHobbyWriter.changePlayerHobby(
                playerId,
                command.hobbyId(),
                command.name(),
                command.detail(),
                command.proficiency(),
                PlayerHobbyStatus.parse(command.status()),
                command.startedOn()
        );

        return PlayerHobbyResult.ChangedPlayerHobby.from(playerHobby);
    }

    @Transactional
    public void deletePlayerHobby(Long playerId, Long hobbyId) {
        playerHobbyWriter.deletePlayerHobby(playerId, hobbyId);
    }
}
