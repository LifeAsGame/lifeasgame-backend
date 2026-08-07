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
public class RoleRelationType {

    @Column(name = "relation_type", nullable = false, length = 40)
    private String value;

    private RoleRelationType(String raw) {
        if (raw == null) {
            throw new DomainException(RoleError.INVALID_ROLE_RELATION_TYPE);
        }
        String normalized = raw.strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 40) {
            throw new DomainException(RoleError.INVALID_ROLE_RELATION_TYPE);
        }
        this.value = normalized;
    }

    public static RoleRelationType of(String raw) {
        return new RoleRelationType(raw);
    }

    public String value() {
        return value;
    }
}
