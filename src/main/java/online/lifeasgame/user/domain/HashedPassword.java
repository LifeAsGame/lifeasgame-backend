package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HashedPassword {
    @Column(name="password_hash", length=100, nullable=false)
    private String value;

    private HashedPassword(String value) {
        this.value = value;
    }

    public static HashedPassword of(String hash) {
        Guard.notNull(hash, "hashedPassword");
        if (hash.length() < 50) throw new IllegalArgumentException("invalid hash");
        return new HashedPassword(hash);
    }
}
