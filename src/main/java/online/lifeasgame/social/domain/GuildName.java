package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuildName {

    @Column(name = "name", length = 60, nullable = false, unique = true)
    private String value;

    private GuildName(String value) {
        this.value = value;
    }

    public static GuildName of(String raw) {
        Guard.notBlank(raw, "guild name");
        String trimmed = raw.strip();
        Guard.maxLength(trimmed, 60, "guild name");
        return new GuildName(trimmed);
    }

    public String value() { return value; }

    @Override public boolean equals(Object o){
        if(this==o) return true; if(!(o instanceof GuildName that)) return false;
        return Objects.equals(value, that.value);
    }
    @Override public int hashCode(){ return Objects.hash(value); }
    @Override public String toString(){ return value; }
}
