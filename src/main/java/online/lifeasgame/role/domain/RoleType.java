package online.lifeasgame.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.error.RoleError;

import java.util.Locale;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleType {

    @Column(name = "role_type", nullable = false, length = 40)
    private String value;

    private RoleType(String raw) {
        if (raw == null) {
            throw new DomainException(RoleError.INVALID_ROLE_TYPE);
        }
        String normalized = raw.strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 40) {
            throw new DomainException(RoleError.INVALID_ROLE_TYPE);
        }
        this.value = normalized;
    }

    public static RoleType of(String raw) {
        return new RoleType(raw);
    }

    public String value() {
        return value;
    }
}
