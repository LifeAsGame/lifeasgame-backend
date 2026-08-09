package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.auth.application.internal.AuthTokenApi;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.core.security.CurrentUserAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerFacade {

    private final CurrentUserAccessor currentUserAccessor;
    private final PlayerService playerService;
    private final PlayerEquipmentService playerEquipmentService;
    private final AuthTokenApi authTokenApi;

    //TODO: 이벤트 처리로 status 전이 방식으로 생성처리 방식 구성
    public PlayerResult.CreatedWithToken linkStart(PlayerCommand.Register command) {
        Long userId = currentUserAccessor.currentUserIdOrThrow();
        PlayerResult.Created created = playerService.linkStart(userId, command);
        AuthResult.TokenPair tokenPair = authTokenApi.issueToken(userId, created.id());
        playerEquipmentService.init(created.id());
        return new PlayerResult.CreatedWithToken(created.id(), tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
