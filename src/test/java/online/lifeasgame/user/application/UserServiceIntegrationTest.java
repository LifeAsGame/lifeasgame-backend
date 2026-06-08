package online.lifeasgame.user.application;

import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.support.IntegrationTestConfig;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
@Transactional
class UserServiceIntegrationTest {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;

    private static final String EMAIL = "integ@test.com";
    private static final String PW    = "password1";
    private static final String NICK  = "IntegUser";

    @Test @DisplayName("register → DB 저장, 이메일/닉네임 확인")
    void register_persists() {
        UserResult.Created r = userService.register(new UserCommand.Register(EMAIL,PW,NICK));
        User found = userRepository.findById(r.id()).orElseThrow();
        assertThat(found.getEmail().getValue()).isEqualTo(EMAIL);
        assertThat(found.getNickname().getValue()).isEqualTo(NICK);
    }

    @Test @DisplayName("register → findAuthCredential → 동일 userId")
    void register_thenFindCredential() {
        UserResult.Created created = userService.register(new UserCommand.Register(EMAIL,PW,NICK));
        assertThat(userService.findAuthCredential(EMAIL,PW).userId()).isEqualTo(created.id());
    }

    @Test @DisplayName("잘못된 비밀번호 → AuthException")
    void wrongPassword() {
        userService.register(new UserCommand.Register(EMAIL,PW,NICK));
        assertThatThrownBy(() -> userService.findAuthCredential(EMAIL,"wrongpass1"))
                .isInstanceOf(AuthException.class);
    }

    @Test @DisplayName("이메일 중복 확인 lifecycle")
    void emailAvailability() {
        assertThat(userService.checkEmailAvailability(EMAIL).isAvailable()).isTrue();
        userService.register(new UserCommand.Register(EMAIL,PW,NICK));
        assertThat(userService.checkEmailAvailability(EMAIL).isAvailable()).isFalse();
    }
}
