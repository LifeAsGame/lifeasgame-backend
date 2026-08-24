package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.internal.UserAuthApi;
import online.lifeasgame.user.application.model.RawPassword;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAuthService implements UserAuthApi {

    private final UserService userService;
    private final UserWriter userWriter;
    private final UserReader userReader;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional(readOnly = true)
    public Long authenticate(String email, String rawPassword) {
        User user = userReader.findByEmailOrElseThrow(email);

        boolean passwordMatches = passwordHasher.matches(
                RawPassword.of(rawPassword),
                user.getPasswordHash()
        );
        if (!passwordMatches || user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(AuthError.BAD_CREDENTIALS);
        }

        return user.getId();
    }

    @Override
    public Long register(String email, String rawPassword, String nickname) {
        return userService.register(new UserCommand.Register(email, rawPassword, nickname)).id();
    }

    @Override
    @Transactional
    public Long findOrRegisterByGoogle(String email, String name) {
        return userReader.findByEmail(email)
                .map(User::getId)
                .orElseGet(() -> userWriter.registerByOAuth(
                        online.lifeasgame.user.domain.Email.of(email),
                        Nickname.of(resolveUniqueNickname(name))
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountAuthorization> resolveAuthorization(Long userId) {
        return userReader.findById(userId)
                .map(user -> new AccountAuthorization(
                        user.getStatus() == UserStatus.ACTIVE,
                        user.isAdmin()
                ));
    }

    private String resolveUniqueNickname(String base) {
        String compact = base.replaceAll("\\s+", "");
        String candidate = compact.substring(0, Math.min(compact.length(), 12));
        if (!userReader.existsByNickname(Nickname.of(candidate))) {
            return candidate;
        }
        return candidate + "_" + System.currentTimeMillis() % 10000;
    }
}
