package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class GuildName {

    @Column(name = "name_value", nullable = false, length = 128)
    private String value;

    @Column(name = "name_original", nullable = false, length = 128)
    private String original;

    public static GuildName of(String original) {
        Guard.notBlank(original, "name");
        String norm = original.trim();
        return new GuildName(norm.toLowerCase(), norm);
    }

    private GuildName(String value, String original) {
        this.value = value;
        this.original = original;
    }
}
