package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.auth.application.AuthService;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.core.security.CurrentUserAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerFacade {

    private final CurrentUserAccessor currentUserAccessor;
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final PlayerService playerService;
    private final PlayerEquipmentService playerEquipmentService;
    private final AuthService authService;

    //TODO: 이벤트 처리로 status 전이 방식으로 생성처리 방식 구성
    public PlayerResult.CreatedWithToken linkStart(PlayerCommand.Register command) {
        Long userId = currentUserAccessor.currentUserIdOrThrow();
        PlayerResult.Created created = playerService.linkStart(userId, command);
        AuthResult.TokenPair tokenPair = authService.issueToken(userId, created.id());
        playerEquipmentService.init(created.id());
        return new PlayerResult.CreatedWithToken(created.id(), tokenPair.accessToken(), tokenPair.refreshToken());
    }

    public PlayerResult.PlayerInfo getPlayerInfo() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return playerService.getPlayerInfo(playerId);
    }

    public PlayerResult.UpdatedTitle changeRepresentativeTitle(Long titleId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return playerService.changeRepresentativeTitle(playerId, titleId);
    }
}
