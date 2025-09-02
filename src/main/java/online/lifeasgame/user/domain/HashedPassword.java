package online.lifeasgame.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Getter(onMethod_ = @JsonIgnore)
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HashedPassword {

    @Column(name = "password_hash", nullable = false)
    private String value;

    private HashedPassword(String value) {
        Guard.notBlank(value, "hashedPassword");
        Guard.check(value.length() >= 20, "hashedPassword too short");
        this.value = value;
    }

    public static HashedPassword of(String value) {
        return new HashedPassword(value);
    }
}
