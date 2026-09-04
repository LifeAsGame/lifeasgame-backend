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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlayerFacadeTest {

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @Mock
    private PlayerOnboardingInitializer onboardingInitializer;

    @Mock
    private AuthTokenApi authTokenApi;

    @InjectMocks
    private PlayerFacade playerFacade;

    @Test
    void issuesTokenOnlyAfterTransactionalInitializationReturns() {
        PlayerCommand.Register command = new PlayerCommand.Register("player", "MALE");
        given(currentUserAccessor.currentUserIdOrThrow()).willReturn(23L);
        given(onboardingInitializer.initialize(23L, command)).willReturn(
                new PlayerResult.Created(239L)
        );
        given(authTokenApi.issueToken(23L, 239L)).willReturn(
                new AuthResult.TokenPair("access", "refresh", 23L, 239L)
        );

        PlayerResult.CreatedWithToken result = playerFacade.linkStart(command);

        InOrder order = inOrder(onboardingInitializer, authTokenApi);
        order.verify(onboardingInitializer).initialize(23L, command);
        order.verify(authTokenApi).issueToken(23L, 239L);
        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void doesNotIssueTokenWhenInitializationFails() {
        PlayerCommand.Register command = new PlayerCommand.Register(
                "player",
                "MALE"
        );
        given(currentUserAccessor.currentUserIdOrThrow()).willReturn(23L);
        RuntimeException failure = new RuntimeException("slot failure");
        given(onboardingInitializer.initialize(23L, command))
                .willThrow(failure);

        assertThatThrownBy(() ->
                playerFacade.linkStart(command)
        ).isSameAs(failure);

        verifyNoInteractions(authTokenApi);
    }
}
