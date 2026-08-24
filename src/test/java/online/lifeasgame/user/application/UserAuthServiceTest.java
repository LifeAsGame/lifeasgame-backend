package online.lifeasgame.user.application;

import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.internal.UserAuthApi;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.domain.AccountAuthority;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.HashedPassword;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void resolvesActiveUserAuthority() {
        User user = user();
        given(userReader.findById(300L)).willReturn(Optional.of(user));

        assertThat(userAuthService.resolveAuthorization(300L))
                .contains(new UserAuthApi.AccountAuthorization(true, false));
    }

    @Test
    void authenticatesActiveUserWithExistingIdentity() {
        User user = user();
        given(userReader.findByEmailOrElseThrow("user@example.com"))
                .willReturn(user);
        given(passwordHasher.matches(any(), any())).willReturn(true);

        assertThat(userAuthService.authenticate(
                "user@example.com",
                "password1"
        )).isEqualTo(300L);
    }

    @Test
    void resolvesActiveAdminAuthority() {
        User user = user();
        ReflectionTestUtils.setField(
                user,
                "accountAuthority",
                AccountAuthority.ADMIN
        );
        given(userReader.findById(300L)).willReturn(Optional.of(user));

        assertThat(userAuthService.resolveAuthorization(300L))
                .contains(new UserAuthApi.AccountAuthorization(true, true));
    }

    @Test
    void rejectsInactiveAccountAuthentication() {
        User user = user();
        user.changeStatus(UserStatus.BANNED);
        given(userReader.findByEmailOrElseThrow("user@example.com"))
                .willReturn(user);
        given(passwordHasher.matches(any(), any())).willReturn(true);

        assertThatThrownBy(() -> userAuthService.authenticate(
                "user@example.com",
                "password1"
        )).isInstanceOfSatisfying(
                AuthException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(AuthError.BAD_CREDENTIALS)
        );
    }

    private User user() {
        User user = User.register(
                Email.of("user@example.com"),
                HashedPassword.of("hashedpassword1234567"),
                Nickname.of("player")
        );
        ReflectionTestUtils.setField(user, "id", 300L);
        return user;
    }
}
