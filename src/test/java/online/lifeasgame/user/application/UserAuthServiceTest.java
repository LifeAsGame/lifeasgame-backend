package online.lifeasgame.user.application;

import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.internal.UserAuthApi;
import online.lifeasgame.user.application.result.UserResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserWriter userWriter;

    @Mock
    private UserReader userReader;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private UserAuthService userAuthService;

    @Test
    void implementsProviderOwnedContractAndRegistersThroughUserCommandService() {
        given(userService.register(any(UserCommand.Register.class)))
                .willReturn(new UserResult.Created(239L));

        UserAuthApi api = userAuthService;
        Long userId = api.register("user@example.com", "password1", "player");

        assertThat(userId).isEqualTo(239L);
        verify(userService).register(new UserCommand.Register(
                "user@example.com",
                "password1",
                "player"
        ));
    }
}
