package online.lifeasgame.user.application;

import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.model.RawPassword;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.domain.*;
import online.lifeasgame.user.domain.error.UserError;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final HashedPassword TEST_HASH =
            HashedPassword.of("$2a$10$testhashedpassword12");

    @Mock UserWriter userWriter;
    @Mock UserReader userReader;
    @Mock PasswordHasher passwordHasher;
    @Mock CurrentUserAccessor currentUserAccessor;
    @InjectMocks UserService userService;
    @InjectMocks UserAuthService userAuthService;
    @InjectMocks UserQueryService userQueryService;

    // ── register() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("정상 → Created(userId) 반환")
        void success() {
            when(passwordHasher.hash(any(RawPassword.class))).thenReturn(TEST_HASH);
            when(userWriter.register(
                    any(Email.class),
                    any(HashedPassword.class),
                    any(Nickname.class)
            )).thenReturn(1L);

            UserResult.Created result = userService.register(
                    new UserCommand.Register("e@e.com", "password1", "Nick"));

            assertThat(result.id()).isEqualTo(1L);
            verify(passwordHasher).hash(any(RawPassword.class));
            verify(userWriter).register(any(), any(), any());
        }
    }

    // ── authenticate() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authenticate()")
    class FindAuthCredential {

        @Test
        @DisplayName("이메일 없음 → DomainException(USER_NOT_FOUND)")
        void emailNotFound() {
            when(userReader.findByEmailOrElseThrow(anyString()))
                    .thenThrow(new DomainException(UserError.USER_NOT_FOUND));

            assertThatThrownBy(() -> userAuthService.authenticate("x@x.com", "password1"))
                    .isInstanceOf(DomainException.class);

            // 비밀번호 검증까지 도달하지 않음
            verifyNoInteractions(passwordHasher);
        }

        @Test
        @DisplayName("비밀번호 불일치 → AuthException(BAD_CREDENTIALS)")
        void wrongPassword() {
            User u = mock(User.class);
            when(u.getPasswordHash()).thenReturn(TEST_HASH);
            when(userReader.findByEmailOrElseThrow(anyString())).thenReturn(u);
            when(passwordHasher.matches(any(RawPassword.class), any(HashedPassword.class)))
                    .thenReturn(false);

            assertThatThrownBy(() -> userAuthService.authenticate("x@x.com", "wrongpass1"))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("정상 → AuthCredential(userId) 반환")
        void success() {
            User u = mock(User.class);
            when(u.getId()).thenReturn(1L);
            when(u.getPasswordHash()).thenReturn(TEST_HASH);
            when(u.getStatus()).thenReturn(UserStatus.ACTIVE);
            when(userReader.findByEmailOrElseThrow(anyString())).thenReturn(u);
            when(passwordHasher.matches(any(RawPassword.class), any(HashedPassword.class)))
                    .thenReturn(true);

            Long userId = userAuthService.authenticate("x@x.com", "password1");

            assertThat(userId).isEqualTo(1L);
        }
    }

    // ── checkEmailAvailability() ──────────────────────────────────────────────

    @Nested
    @DisplayName("checkEmailAvailability()")
    class EmailAvailability {

        @Test
        @DisplayName("미사용 이메일 → available=true")
        void available() {
            when(userReader.existsByEmail(any(Email.class))).thenReturn(false);

            assertThat(userQueryService.checkEmailAvailability("e@e.com").isAvailable()).isTrue();
        }

        @Test
        @DisplayName("중복 이메일 → available=false")
        void duplicate() {
            when(userReader.existsByEmail(any(Email.class))).thenReturn(true);

            assertThat(userQueryService.checkEmailAvailability("e@e.com").isAvailable()).isFalse();
        }
    }

    // ── checkNicknameAvailability() ───────────────────────────────────────────

    @Nested
    @DisplayName("checkNicknameAvailability()")
    class NicknameAvailability {

        @Test
        @DisplayName("미사용 닉네임 → available=true")
        void available() {
            when(userReader.existsByNickname(any(Nickname.class))).thenReturn(false);

            assertThat(userQueryService.checkNicknameAvailability("NewNick").isAvailable()).isTrue();
        }

        @Test
        @DisplayName("중복 닉네임 → available=false")
        void duplicate() {
            when(userReader.existsByNickname(any(Nickname.class))).thenReturn(true);

            assertThat(userQueryService.checkNicknameAvailability("Kirito").isAvailable()).isFalse();
        }
    }

    // ── changeNickname() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("changeNickname()")
    class ChangeNickname {

        @Test
        @DisplayName("정상 → user.changeNickname() 호출 + NicknameChanged 반환")
        void success() {
            User u = mock(User.class);
            when(u.getId()).thenReturn(1L);
            when(u.getNickname()).thenReturn(Nickname.of("NewNick"));
            when(u.getUpdatedAt()).thenReturn(Instant.now());
            when(userReader.findByIdOrElseThrow(1L)).thenReturn(u);
            when(currentUserAccessor.currentUserIdOrThrow()).thenReturn(1L);

            UserResult.NicknameChanged result = userService.changeNickname("NewNick");

            verify(u).changeNickname(any(Nickname.class));
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.newNickname()).isEqualTo("NewNick");
        }

        @Test
        @DisplayName("존재하지 않는 userId → DomainException 전파")
        void userNotFound() {
            when(userReader.findByIdOrElseThrow(999L))
                    .thenThrow(new DomainException(UserError.USER_NOT_FOUND));

            assertThatThrownBy(() -> userService.changeNickname(999L, "NewNick"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("현재 사용자 identity로 삭제한다")
        void currentUser() {
            User user = mock(User.class);
            when(currentUserAccessor.currentUserIdOrThrow()).thenReturn(1L);
            when(userReader.findByIdOrElseThrow(1L)).thenReturn(user);
            when(passwordHasher.hash(any(RawPassword.class))).thenReturn(TEST_HASH);
            when(user.getId()).thenReturn(1L);
            when(user.getStatus()).thenReturn(UserStatus.DELETED);

            UserResult.Deleted result = userService.delete("password1");

            verify(user).delete(TEST_HASH);
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo("DELETED");
        }
    }

    // ── changePassword() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("정상 → user.changePassword() 호출 + PasswordChanged 반환")
        void success() {
            User u = mock(User.class);
            when(u.getId()).thenReturn(1L);
            when(userReader.findByIdOrElseThrow(1L)).thenReturn(u);
            when(passwordHasher.hash(any(RawPassword.class))).thenReturn(TEST_HASH);
            when(currentUserAccessor.currentUserIdOrThrow()).thenReturn(1L);

            UserResult.PasswordChanged result = userService.changePassword(
                    new UserCommand.ChangePassword("currentPw1", "newPassword1"));

            verify(u).changePassword(any(HashedPassword.class), any(HashedPassword.class));
            assertThat(result.userId()).isEqualTo(1L);
        }
    }

    // ── getUserInfo() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserInfo()")
    class GetUserInfo {

        @Test
        @DisplayName("정상 → UserInfo(email, nickname) 반환")
        void success() {
            User u = mock(User.class);
            when(u.getEmail()).thenReturn(Email.of("e@e.com"));
            when(u.getNickname()).thenReturn(Nickname.of("Nick"));
            when(userReader.findByIdOrElseThrow(1L)).thenReturn(u);
            when(currentUserAccessor.currentUserIdOrThrow()).thenReturn(1L);

            UserResult.UserInfo info = userQueryService.getUserInfo();

            assertThat(info.email()).isEqualTo("e@e.com");
            assertThat(info.nickname()).isEqualTo("Nick");
        }

        @Test
        @DisplayName("존재하지 않는 userId → DomainException 전파")
        void notFound() {
            when(userReader.findByIdOrElseThrow(999L))
                    .thenThrow(new DomainException(UserError.USER_NOT_FOUND));

            assertThatThrownBy(() -> userQueryService.getUserInfo(999L))
                    .isInstanceOf(DomainException.class);
        }
    }
}
