package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.model.RawPassword;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserSetting;
import online.lifeasgame.user.domain.error.UserError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserWriter userWriter;
    private final UserReader userReader;
    private final PasswordHasher passwordHasher;
    private final UserSettingReader userSettingReader;

    @Transactional
    public UserResult.Created register(UserCommand.Register register) {
        Long userId = userWriter.register(
                Email.of(register.email()),
                passwordHasher.hash(RawPassword.of(register.password())),
                Nickname.of(register.nickname())
        );

        return new UserResult.Created(userId);
    }

    public UserResult.UserInfo getUserInfo(Long userId) {
        User user = userReader.findByIdOrElseThrow(userId);
        return UserResult.UserInfo.from(user);
    }

    public UserResult.Availability checkEmailAvailability(String email) {
        boolean isAvailable = userReader.existsByEmail(Email.of(email));
        return new UserResult.Availability(isAvailable, UserError.EMAIL_DUPLICATE.message());
    }

    public UserResult.Availability checkNicknameAvailability(String nickname) {
        boolean isAvailable = userReader.existsByNickname(Nickname.of(nickname));
        return new UserResult.Availability(isAvailable, UserError.NICKNAME_DUPLICATE.message());
    }

    public UserResult.NicknameChanged changeNickname(Long userId, String nickname) {
        User user = userReader.findByIdOrElseThrow(userId);
        user.changeNickname(Nickname.of(nickname));
        return new UserResult.NicknameChanged(user.getId(), user.getNickname().getValue());
    }

    public UserResult.PasswordChanged changePassword(Long userId, UserCommand.ChangePassword command) {
        User user = userReader.findByIdOrElseThrow(userId);
        user.changePassword(
                passwordHasher.hash(RawPassword.of(command.currentPassword())),
                passwordHasher.hash(RawPassword.of(command.newPassword()))
        );

        return new UserResult.PasswordChanged(user.getId());
    }

    public UserResult.Deleted delete(Long userId, String password) {
        User user = userReader.findByIdOrElseThrow(userId);
        user.delete(passwordHasher.hash(RawPassword.of(password)));
        return new UserResult.Deleted(user.getId(), user.getStatus().name());
    }

    public UserResult.Settings getSettings(Long userId) {
        UserSetting userSetting = userSettingReader.findByIdOrElseThrow(userId);
        return UserResult.Settings.from(userSetting);
    }
}
