package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.model.RawPassword;
import online.lifeasgame.user.application.query.UserSearchQuery;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserStatus;
import online.lifeasgame.user.domain.error.UserError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserWriter userWriter;
    private final UserReader userReader;
    private final PasswordHasher passwordHasher;

    @Transactional
    public UserResult.Created register(UserCommand.Register register) {
        Long userId = userWriter.register(
                Email.of(register.email()),
                passwordHasher.hash(RawPassword.of(register.password())),
                Nickname.of(register.nickname())
        );

        return new UserResult.Created(userId);
    }

    @Transactional
    public UserResult.AuthCredential findOrRegisterByGoogle(String email, String name) {
        return userReader.findByEmail(email)
                .map(user -> new UserResult.AuthCredential(user.getId()))
                .orElseGet(() -> {
                    String nickname = resolveUniqueNickname(name);
                    Long userId = userWriter.registerByOAuth(
                            Email.of(email),
                            Nickname.of(nickname)
                    );
                    return new UserResult.AuthCredential(userId);
                });
    }

    private String resolveUniqueNickname(String base) {
        String candidate = base.replaceAll("\\s+", "").substring(0, Math.min(base.length(), 12));
        if (!userReader.existsByNickname(Nickname.of(candidate))) return candidate;
        return candidate + "_" + System.currentTimeMillis() % 10000;
    }

    public UserResult.UserInfo getUserInfo(Long userId) {
        User user = userReader.findByIdOrElseThrow(userId);
        return UserResult.UserInfo.from(user);
    }

    public UserResult.Availability checkEmailAvailability(String email) {
        boolean isAvailable = !userReader.existsByEmail(Email.of(email));
        return new UserResult.Availability(isAvailable, UserError.EMAIL_DUPLICATE.message());
    }

    public UserResult.Availability checkNicknameAvailability(String nickname) {
        boolean isAvailable = !userReader.existsByNickname(Nickname.of(nickname));
        return new UserResult.Availability(isAvailable, UserError.NICKNAME_DUPLICATE.message());
    }

    @Transactional(readOnly = true)
    public UserResult.AuthCredential findAuthCredential(String email, String rawPassword) {
        User user = userReader.findByEmailOrElseThrow(email);

        if (!passwordHasher.matches(RawPassword.of(rawPassword), user.getPasswordHash())) {
            throw new AuthException(AuthError.BAD_CREDENTIALS);
        }

        return new UserResult.AuthCredential(user.getId());
    }

    @Transactional
    public UserResult.NicknameChanged changeNickname(Long userId, String nickname) {
        User user = userReader.findByIdOrElseThrow(userId);
        user.changeNickname(Nickname.of(nickname));

        return new UserResult.NicknameChanged(
                user.getId(),
                nickname,
                user.getNickname().getValue(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public UserResult.PasswordChanged changePassword(Long userId, UserCommand.ChangePassword command) {
        User user = userReader.findByIdOrElseThrow(userId);
        user.changePassword(
                passwordHasher.hash(RawPassword.of(command.currentPassword())),
                passwordHasher.hash(RawPassword.of(command.newPassword()))
        );

        return new UserResult.PasswordChanged(user.getId());
    }

    @Transactional
    public UserResult.StatusChanged changeStatus(Long userId, UserCommand.ChangeStatus command) {
        User user = userReader.findByIdOrElseThrow(userId);
        user.changeStatus(UserStatus.parse(command.status()));

        return new UserResult.StatusChanged(
                user.getId(),
                command.status(),
                user.getStatus().name(),
                command.reason(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public UserResult.Deleted delete(Long userId, String password) {
        User user = userReader.findByIdOrElseThrow(userId);
        user.delete(passwordHasher.hash(RawPassword.of(password)));
        return new UserResult.Deleted(user.getId(), user.getStatus().name());
    }

    @Transactional(readOnly = true)
    public UserResult.UserList search(UserCommand.Search command) {
        int safePage = Math.max(command.page(), 0);
        int safeSize = Math.min(Math.max(command.size(), 1), 100);

        UserSearchQuery.SearchResult result = userReader.search(
                command.email(),
                command.nickname(),
                UserStatus.parseNullable(command.status()),
                safePage,
                safeSize
        );

        return UserResult.UserList.from(result.users(), safePage, safeSize, result.total());
    }
}
