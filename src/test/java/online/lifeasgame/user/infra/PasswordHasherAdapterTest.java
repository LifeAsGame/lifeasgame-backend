package online.lifeasgame.user.infra;

import online.lifeasgame.user.application.model.RawPassword;
import online.lifeasgame.user.domain.HashedPassword;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;

/**
 * PasswordHasherAdapter 단위 테스트.
 * 실제 BCryptPasswordEncoder 사용 — Mock 아님.
 */
class PasswordHasherAdapterTest {

    private final PasswordHasherAdapter adapter =
            new PasswordHasherAdapter(new BCryptPasswordEncoder());

    @Test
    @DisplayName("hash() → BCrypt 포맷($2a$) 반환")
    void hash_returnsBcrypt() {
        HashedPassword hashed = adapter.hash(RawPassword.of("password1"));
        assertThat(hashed.getValue()).startsWith("$2a$");
    }

    @Test
    @DisplayName("같은 원문 반복 해시 → 결과가 다름 (salt)")
    void hash_differentEachTime() {
        RawPassword raw = RawPassword.of("password1");
        assertThat(adapter.hash(raw).getValue())
                .isNotEqualTo(adapter.hash(raw).getValue());
    }

    @Test
    @DisplayName("matches() — 동일 원문 → true")
    void matches_sameRaw_true() {
        RawPassword raw = RawPassword.of("password1");
        HashedPassword hashed = adapter.hash(raw);
        assertThat(adapter.matches(raw, hashed)).isTrue();
    }

    @Test
    @DisplayName("matches() — 다른 원문 → false")
    void matches_differentRaw_false() {
        HashedPassword hashed = adapter.hash(RawPassword.of("password1"));
        assertThat(adapter.matches(RawPassword.of("password2"), hashed)).isFalse();
    }
}
