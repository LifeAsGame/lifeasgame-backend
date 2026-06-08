package online.lifeasgame.user.domain;

import online.lifeasgame.user.application.model.RawPassword;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class ValueObjectTest {

    @Nested
    @DisplayName("Email")
    class EmailTest {

        @ParameterizedTest
        @ValueSource(strings = {"user@test.com", "a.b+c@domain.co.kr"})
        @DisplayName("유효한 이메일 → 정상 생성")
        void valid(String email) {
            assertThatCode(() -> Email.of(email)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-an-email", "@nodomain", "missing@", " ", ""})
        @DisplayName("잘못된 이메일 → 예외")
        void invalid(String email) {
            assertThatThrownBy(() -> Email.of(email)).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("대소문자 혼합 → 소문자 정규화")
        void lowercased() {
            assertThat(Email.of("Test@Example.COM").getValue())
                    .isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Nickname")
    class NicknameTest {

        @ParameterizedTest
        @ValueSource(strings = {"ab", "Kirito", "TwentyCharNickname!!"})
        @DisplayName("2~20자 → 정상")
        void valid(String nick) {
            assertThatCode(() -> Nickname.of(nick)).doesNotThrowAnyException();
        }

        @Test @DisplayName("1자 → 예외") void tooShort() {
            assertThatThrownBy(() -> Nickname.of("a")).isInstanceOf(Exception.class);
        }

        @Test @DisplayName("21자 → 예외") void tooLong() {
            assertThatThrownBy(() -> Nickname.of("a".repeat(21))).isInstanceOf(Exception.class);
        }

        @Test @DisplayName("공백 → 예외") void blank() {
            assertThatThrownBy(() -> Nickname.of("   ")).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("RawPassword")
    class RawPasswordTest {

        @Test @DisplayName("8자 영숫자 → 정상") void valid() {
            assertThatCode(() -> RawPassword.of("password1")).doesNotThrowAnyException();
        }

        @Test @DisplayName("7자 → 예외") void tooShort() {
            assertThatThrownBy(() -> RawPassword.of("pass123")).isInstanceOf(Exception.class);
        }

        @Test @DisplayName("특수문자 포함 → 예외") void specialChar() {
            assertThatThrownBy(() -> RawPassword.of("pass!word1")).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("HashedPassword")
    class HashedPasswordTest {

        @Test @DisplayName("20자 이상 → 정상") void valid() {
            assertThatCode(() -> HashedPassword.of("a".repeat(20))).doesNotThrowAnyException();
        }

        @Test @DisplayName("19자 → 예외") void tooShort() {
            assertThatThrownBy(() -> HashedPassword.of("a".repeat(19))).isInstanceOf(Exception.class);
        }

        @Test @DisplayName("oauthPlaceholder → isOAuthAccount=true") void oauth() {
            assertThat(HashedPassword.oauthPlaceholder().isOAuthAccount()).isTrue();
        }

        @Test @DisplayName("일반 해시 → isOAuthAccount=false") void notOauth() {
            assertThat(HashedPassword.of("a".repeat(20)).isOAuthAccount()).isFalse();
        }
    }
}
