package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email {

    @Column(name = "email", nullable = false)
    public String email;

    private Email(String email) {
        this.email = email;
    }

    public static Email of(String email) {
        Objects.requireNonNull(email);
        var v = email.trim().toLowerCase(Locale.ROOT);
        if (!v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("invalid email: " + email);
        return new Email(v);
    }
}
