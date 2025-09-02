package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email {

    @Column(name = "email", nullable = false, length = 254)
    private String value;

    private Email(String raw) {
        String value = Guard.notBlank(raw, "email").toLowerCase(Locale.ROOT);
        Guard.check(value.length() <= 254, "email too long");
        Guard.check(value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "invalid email");
        this.value = value;
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
