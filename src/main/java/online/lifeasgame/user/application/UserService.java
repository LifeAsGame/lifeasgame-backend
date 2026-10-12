package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.model.RawPassword;
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
    private final CurrentUserAccessor currentUserAccessor;

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
    public UserResult.NicknameChanged changeNickname(String nickname) {
        return changeNickname(currentUserAccessor.currentUserIdOrThrow(), nickname);
    }

    @Transactional
    public UserResult.NicknameChanged changeNickname(Long userId, String nickname) {
        User user = userReader.findByIdOrElseThrow(userId);
        String oldNickname = user.getNickname().getValue();
        user.changeNickname(Nickname.of(nickname));

        return new UserResult.NicknameChanged(
                user.getId(),
                oldNickname,
                user.getNickname().getValue(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public UserResult.PasswordChanged changePassword(UserCommand.ChangePassword command) {
        Long userId = currentUserAccessor.currentUserIdOrThrow();
        User user = userReader.findByIdOrElseThrow(userId);
        if (user.isOAuthAccount()
                || !passwordHasher.matches(RawPassword.of(command.currentPassword()), user.getPasswordHash())) {
            throw new DomainException(UserError.INCORRECT_PASSWORD);
        }
        user.changePassword(passwordHasher.hash(RawPassword.of(command.newPassword())));

        return new UserResult.PasswordChanged(user.getId());
    }

    @Transactional
    public UserResult.StatusChanged changeStatus(Long userId, UserCommand.ChangeStatus command) {
        User user = userReader.findByIdOrElseThrow(userId);
        String previousStatus = user.getStatus().name();
        user.changeStatus(UserStatus.parse(command.status()));

        return new UserResult.StatusChanged(
                user.getId(),
                previousStatus,
                user.getStatus().name(),
                command.reason(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public UserResult.Deleted delete(String password) {
        Long userId = currentUserAccessor.currentUserIdOrThrow();
        User user = userReader.findByIdOrElseThrow(userId);
        if (user.isOAuthAccount()
                || !passwordHasher.matches(RawPassword.of(password), user.getPasswordHash())) {
            throw new DomainException(UserError.INCORRECT_PASSWORD);
        }
        user.deleteByOwner();
        return new UserResult.Deleted(user.getId(), user.getStatus().name());
    }
}
