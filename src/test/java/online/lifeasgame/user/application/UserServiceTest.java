package online.lifeasgame.user.application;

import online.lifeasgame.character.application.internal.PlayerLookupApi;
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
import org.springframework.test.util.ReflectionTestUtils;

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
    @Mock PlayerLookupApi playerLookupApi;
    @InjectMocks UserService userService;
    @InjectMocks UserAuthService userAuthService;
    @InjectMocks UserQueryService userQueryService;

    private final PasswordHasher realHasher =
            new online.lifeasgame.user.infra.PasswordHasherAdapter(
                    org.springframework.security.crypto.factory.PasswordEncoderFactories
                            .createDelegatingPasswordEncoder());

    private User storedUser() {
        User user = User.register(Email.of("e@e.com"), realHasher.hash(RawPassword.of("password1")),
                Nickname.of("Hero"));
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        when(currentUserAccessor.currentUserIdOrThrow()).thenReturn(1L);
        when(userReader.findByIdOrElseThrow(1L)).thenReturn(user);
        return user;
    }

    private UserService passwordService() {
        return new UserService(userWriter, userReader, realHasher, currentUserAccessor);
    }

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

            UserResult.Availability result = userQueryService.checkEmailAvailability("e@e.com");
            assertThat(result.isAvailable()).isTrue();
            assertThat(result.reason()).isNull();
        }

        @Test
        @DisplayName("중복 이메일 → available=false")
        void duplicate() {
            when(userReader.existsByEmail(any(Email.class))).thenReturn(true);

            UserResult.Availability result = userQueryService.checkEmailAvailability("e@e.com");
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.reason()).isEqualTo(UserError.EMAIL_DUPLICATE);
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

            UserResult.Availability result = userQueryService.checkNicknameAvailability("NewNick");
            assertThat(result.isAvailable()).isTrue();
            assertThat(result.reason()).isNull();
        }

        @Test
        @DisplayName("중복 닉네임 → available=false")
        void duplicate() {
            when(userReader.existsByNickname(any(Nickname.class))).thenReturn(true);

            UserResult.Availability result = userQueryService.checkNicknameAvailability("Kirito");
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.reason()).isEqualTo(UserError.NICKNAME_DUPLICATE);
        }
    }

    // ── changeNickname() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("changeNickname()")
    class ChangeNickname {

        @Test
        @DisplayName("변경 전후 닉네임을 각각 응답한다")
        void success() {
            User u = User.register(Email.of("e@e.com"), TEST_HASH, Nickname.of("OldNick"));
            ReflectionTestUtils.setField(u, "id", 1L);
            when(userReader.findByIdOrElseThrow(1L)).thenReturn(u);
            when(currentUserAccessor.currentUserIdOrThrow()).thenReturn(1L);

            UserResult.NicknameChanged result = userService.changeNickname("NewNick");

            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.oldNickname()).isEqualTo("OldNick");
            assertThat(result.newNickname()).isEqualTo("NewNick");
            assertThat(u.getNickname().getValue()).isEqualTo("NewNick");
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
    @DisplayName("계정 상태를 변경할 때")
    class ChangeStatus {

        @Test
        @DisplayName("변경 전후 상태를 각각 응답한다")
        void reportsTransition() {
            User user = User.register(Email.of("e@e.com"), TEST_HASH, Nickname.of("OldNick"));
            ReflectionTestUtils.setField(user, "id", 1L);
            when(userReader.findByIdOrElseThrow(1L)).thenReturn(user);

            UserResult.StatusChanged result = userService.changeStatus(
                    1L, new UserCommand.ChangeStatus("BANNED", "moderation"));

            assertThat(result.fromStatus()).isEqualTo("ACTIVE");
            assertThat(result.toStatus()).isEqualTo("BANNED");
            assertThat(user.getStatus()).isEqualTo(UserStatus.BANNED);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("현재 비밀번호가 맞으면 현재 계정을 삭제한다")
        void currentPassword() {
            User user = storedUser();

            UserResult.Deleted result = passwordService().delete("password1");

            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo("DELETED");
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 계정을 유지하고 오류를 반환한다")
        void wrongPassword() {
            User user = storedUser();

            assertThatThrownBy(() -> passwordService().delete("wrongpass1"))
                    .isInstanceOfSatisfying(DomainException.class, error ->
                            assertThat(error.getErrorCode()).isEqualTo(UserError.INCORRECT_PASSWORD));
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }

    // ── changePassword() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("현재 비밀번호가 맞으면 새 비밀번호로 변경한다")
        void success() {
            User user = storedUser();

            UserResult.PasswordChanged result = passwordService().changePassword(
                    new UserCommand.ChangePassword("password1", "newPassword1"));

            assertThat(result.userId()).isEqualTo(1L);
            assertThat(realHasher.matches(RawPassword.of("newPassword1"), user.getPasswordHash())).isTrue();
            assertThat(realHasher.matches(RawPassword.of("password1"), user.getPasswordHash())).isFalse();
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 기존 비밀번호를 유지한다")
        void wrongPassword() {
            User user = storedUser();

            assertThatThrownBy(() -> passwordService().changePassword(
                    new UserCommand.ChangePassword("wrongpass1", "newPassword1")))
                    .isInstanceOfSatisfying(DomainException.class, error ->
                            assertThat(error.getErrorCode()).isEqualTo(UserError.INCORRECT_PASSWORD));
            assertThat(realHasher.matches(RawPassword.of("password1"), user.getPasswordHash())).isTrue();
        }
    }

    // ── getUserInfo() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("현재 사용자 정보를 조회할 때")
    class GetUserInfo {

        @Test
        @DisplayName("연결된 Player가 없으면 실제 User 정보와 null playerId를 반환한다")
        void withoutPlayer() {
            User u = mock(User.class);
            when(u.getId()).thenReturn(1L);
            when(u.getEmail()).thenReturn(Email.of("e@e.com"));
            when(u.getNickname()).thenReturn(Nickname.of("Nick"));
            when(u.getStatus()).thenReturn(UserStatus.ACTIVE);
            when(userReader.findByIdOrElseThrow(1L)).thenReturn(u);
            when(currentUserAccessor.currentUserIdOrThrow()).thenReturn(1L);
            when(playerLookupApi.findPlayerIdByUserId(1L)).thenReturn(null);

            UserResult.UserInfo info = userQueryService.getUserInfo();

            assertThat(info.userId()).isEqualTo(1L);
            assertThat(info.email()).isEqualTo("e@e.com");
            assertThat(info.nickname()).isEqualTo("Nick");
            assertThat(info.status()).isEqualTo("ACTIVE");
            assertThat(info.playerId()).isNull();
            verify(playerLookupApi).findPlayerIdByUserId(1L);
        }

        @Test
        @DisplayName("연결된 Player가 있으면 Character 경계가 반환한 playerId를 사용한다")
        void withPlayer() {
            User u = mock(User.class);
            when(u.getId()).thenReturn(1L);
            when(u.getEmail()).thenReturn(Email.of("e@e.com"));
            when(u.getNickname()).thenReturn(Nickname.of("Nick"));
            when(u.getStatus()).thenReturn(UserStatus.ACTIVE);
            when(userReader.findByIdOrElseThrow(1L)).thenReturn(u);
            when(currentUserAccessor.currentUserIdOrThrow()).thenReturn(1L);
            when(playerLookupApi.findPlayerIdByUserId(1L)).thenReturn(77L);

            UserResult.UserInfo info = userQueryService.getUserInfo();

            assertThat(info.playerId()).isEqualTo(77L);
            verify(playerLookupApi).findPlayerIdByUserId(1L);
        }

        @Test
        @DisplayName("존재하지 않는 userId → DomainException 전파")
        void notFound() {
            when(userReader.findByIdOrElseThrow(999L))
                    .thenThrow(new DomainException(UserError.USER_NOT_FOUND));

            assertThatThrownBy(() -> userQueryService.getUserInfo(999L))
                    .isInstanceOf(DomainException.class);
            verifyNoInteractions(playerLookupApi);
        }
    }
}
