package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.model.RawPassword;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.Nickname;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserWriter userWriter;
    private final PasswordHasher passwordHasher;

    @Transactional
    public UserResult.Created register(UserCommand.Register register) {
        return UserResult.Created.of(
                userWriter.register(
                    Email.of(register.email()),
                    passwordHasher.hash(RawPassword.of(register.password())),
                    Nickname.of(register.nickname())
                )
        );
    }
}
