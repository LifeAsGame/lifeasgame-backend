package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HashedPassword {
    @Column(name="password_hash", length=100, nullable=false)
    private String passwordHash;

    private HashedPassword(String passwordHash) {
        Guard.notNull(passwordHash, "hashedPassword");
        Guard.check(passwordHash.length() >= 50 && passwordHash.length() <= 100, "invalid hash length");
        this.passwordHash = passwordHash;
    }

    public static HashedPassword of(String passwordHash) {
        return new HashedPassword(passwordHash);
    }
}
