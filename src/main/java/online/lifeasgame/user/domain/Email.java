package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email {

    @Column(name = "email", nullable = false, length = 254)
    public String email;

    private Email(String raw) {
        String email = Guard.notBlank(raw, "email").toLowerCase(Locale.ROOT);
        Guard.check(email.length() <= 254, "email too long");
        Guard.check(email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "invalid email");
        this.email = email;
    }

    public static Email of(String email) {
        return new Email(email);
    }
}
