package online.lifeasgame.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

import java.util.Locale;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleType {

    @Column(name = "role_type", nullable = false, length = 40)
    private String value;

    private RoleType(String raw) {
        String normalized = Guard.notBlank(raw, "roleType")
                .toUpperCase(Locale.ROOT);
        this.value = Guard.maxLength(normalized, 40, "roleType");
    }

    public static RoleType of(String raw) {
        return new RoleType(raw);
    }

    public String value() {
        return value;
    }
}
