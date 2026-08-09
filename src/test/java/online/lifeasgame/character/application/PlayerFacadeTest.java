package online.lifeasgame.character.application;

import online.lifeasgame.auth.application.internal.AuthTokenApi;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.core.security.CurrentUserAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class PlayerFacadeTest {

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @Mock
    private PlayerService playerService;

    @Mock
    private PlayerEquipmentService playerEquipmentService;

    @Mock
    private AuthTokenApi authTokenApi;

    @InjectMocks
    private PlayerFacade playerFacade;

    @Test
    void onboardingKeepsCreateTokenAndEquipmentOrchestrationOrder() {
        PlayerCommand.Register command = new PlayerCommand.Register("player", "MALE");
        given(currentUserAccessor.currentUserIdOrThrow()).willReturn(23L);
        given(playerService.linkStart(23L, command)).willReturn(new PlayerResult.Created(239L));
        given(authTokenApi.issueToken(23L, 239L)).willReturn(
                new AuthResult.TokenPair("access", "refresh", 23L, 239L)
        );

        PlayerResult.CreatedWithToken result = playerFacade.linkStart(command);

        InOrder order = inOrder(playerService, authTokenApi, playerEquipmentService);
        order.verify(playerService).linkStart(23L, command);
        order.verify(authTokenApi).issueToken(23L, 239L);
        order.verify(playerEquipmentService).init(239L);
        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }
}
