package online.lifeasgame.user.domain;

import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * User 도메인 단위 테스트.
 * 외부 의존성 없음 — 순수 도메인 로직만 검증.
 */
class UserTest {

    private static User activeUser() {
        return User.register(
                Email.of("test@test.com"),
                HashedPassword.of("hashedpassword1234567"),
                Nickname.of("Kirito")
        );
    }

    @Nested
    @DisplayName("register() — 이메일/비밀번호 가입")
    class Register {

        @Test
        @DisplayName("정상 생성 → ACTIVE / USER 상태")
        void shouldBeActive() {
            User user = activeUser();

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getAccountAuthority())
                    .isEqualTo(AccountAuthority.USER);
            assertThat(user.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("이메일 소문자 정규화")
        void emailLowercased() {
            User u = User.register(
                    Email.of("Test@TEST.com"),
                    HashedPassword.of("hashedpassword1234567"),
                    Nickname.of("Hero"));
            assertThat(u.getEmail().getValue()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("일반 가입 → isOAuthAccount=false")
        void notOAuthAccount() {
            assertThat(activeUser().isOAuthAccount()).isFalse();
        }
    }

/*    @Nested
    @DisplayName("registerByOAuth() — Google OAuth 가입")
    class RegisterByOAuth {

        @Test
        @DisplayName("OAuth 가입 → ACTIVE, isOAuthAccount=true")
        void shouldBeActiveAndOAuth() {
            User u = User.registerByOAuth(Email.of("oauth@google.com"), Nickname.of("Asuna"));
            assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(u.isOAuthAccount()).isTrue();
        }
    }*/

    @Nested
    @DisplayName("changeNickname()")
    class ChangeNickname {

        @Test
        @DisplayName("닉네임 변경 → 반영됨")
        void success() {
            User u = activeUser();
            u.changeNickname(Nickname.of("Asuna"));
            assertThat(u.getNickname().getValue()).isEqualTo("Asuna");
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("현재 비밀번호 일치 → 변경 성공")
        void success() {
            HashedPassword current = HashedPassword.of("hashedpassword1234567");
            HashedPassword next    = HashedPassword.of("newhashedpassword1234");
            User u = User.register(Email.of("a@a.com"), current, Nickname.of("Hero"));
            u.changePassword(current, next);
            assertThat(u.getPasswordHash()).isEqualTo(next);
        }

        @Test
        @DisplayName("비밀번호 불일치 → INCORRECT_PASSWORD 예외")
        void wrongPassword_throws() {
            HashedPassword correct = HashedPassword.of("hashedpassword1234567");
            HashedPassword wrong   = HashedPassword.of("wronghashedpassword12");
            HashedPassword next    = HashedPassword.of("newhashedpassword1234");
            User u = User.register(Email.of("a@a.com"), correct, Nickname.of("Hero"));
            assertThatThrownBy(() -> u.changePassword(wrong, next))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus()")
    class ChangeStatus {

        @Test
        @DisplayName("ACTIVE → BANNED")
        void activeToBanned() {
            User u = activeUser();
            u.changeStatus(UserStatus.BANNED);
            assertThat(u.getStatus()).isEqualTo(UserStatus.BANNED);
        }

        @Test
        @DisplayName("DELETED 상태에서 변경 → 예외")
        void deletedCannotChange() {
            User u = activeUser();
            u.changeStatus(UserStatus.DELETED);
            assertThatThrownBy(() -> u.changeStatus(UserStatus.ACTIVE))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("동일 상태 → 무시")
        void sameStatusIgnored() {
            User u = activeUser();
            assertThatCode(() -> u.changeStatus(UserStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE에서 PENDING_EMAIL_VERIFICATION → 예외 (switch default)")
        void pendingVerification_notSupportedViaChangeStatus() {
            User u = activeUser();
            // changeStatus()는 PENDING_EMAIL_VERIFICATION을 지원하지 않음
            // 메일 인증 활성화 시 register() 팩토리에서 직접 생성
            assertThatThrownBy(() -> u.changeStatus(UserStatus.PENDING_EMAIL_VERIFICATION))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmail {

        @Test
        @DisplayName("ACTIVE 상태에서 verifyEmail() → 예외 (이미 인증 완료)")
        void alreadyActive_throws() {
            // register()는 현재 ACTIVE로 생성됨
            // 메일 인증 활성화 시 PENDING_EMAIL_VERIFICATION으로 생성되는 경로가 별도 존재
            // 여기서는 잘못된 상태에서의 호출 방어를 검증
            User u = activeUser();
            assertThatThrownBy(u::verifyEmail)
                    .isInstanceOf(DomainException.class);
        }
    }
}
