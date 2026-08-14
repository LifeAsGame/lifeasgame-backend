package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerHobbyService {

    private final PlayerHobbyRegistrar playerHobbyRegistrar;
    private final PlayerHobbyUpdater playerHobbyUpdater;
    private final PlayerHobbyRevoker playerHobbyRevoker;
    private final PlayerHobbyReader playerHobbyReader;

    private final HobbyReader hobbyReader;
    private final PlayerReader playerReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public PlayerHobbyResult.Created createPlayerHobby(PlayerHobbyCommand.Create command) {
        return createPlayerHobby(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public PlayerHobbyResult.Created createPlayerHobby(Long playerId, PlayerHobbyCommand.Create command) {
        playerReader.assertExistsById(playerId);

        Hobby hobby = hobbyReader.getByIdOrThrow(command.hobbyId());

        PlayerHobby playerHobby = playerHobbyRegistrar.register(
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

    @Transactional(readOnly = true)
    public List<PlayerHobbyResult.Info> getPlayerHobbyInfos() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<PlayerHobbyView> playerHobbyViews = playerHobbyReader.getViewsByPlayerId(playerId);
        return playerHobbyViews.stream()
                .map(PlayerHobbyResult.Info::from)
                .toList();
    }

    @Transactional
    public PlayerHobbyResult.Changed changePlayerHobby(PlayerHobbyCommand.Change command) {
        return changePlayerHobby(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public PlayerHobbyResult.Changed changePlayerHobby(Long playerId, PlayerHobbyCommand.Change command) {
        PlayerHobby playerHobby = playerHobbyUpdater.update(playerId, command);

        return PlayerHobbyResult.Changed.from(playerHobby);
    }

    @Transactional
    public void deletePlayerHobby(Long hobbyId) {
        deletePlayerHobby(currentPlayerAccessor.currentPlayerIdOrThrow(), hobbyId);
    }

    @Transactional
    public void deletePlayerHobby(Long playerId, Long hobbyId) {
        playerHobbyRevoker.revoke(playerId, hobbyId);
    }

    @Transactional
    public PlayerHobbyResult.Revoked revokeHobby(Long playerId, Long hobbyId) {
        playerHobbyRevoker.revoke(playerId, hobbyId);
        return new PlayerHobbyResult.Revoked(playerId, hobbyId);
    }
}
