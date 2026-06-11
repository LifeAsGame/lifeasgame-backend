package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

import java.util.Objects;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelName {

    private static final int MAX_LENGTH = 60;

    @Column(name = "channel_name", length = MAX_LENGTH)
    private String value;

    private ChannelName(String value) {
        this.value = value;
    }

    public static ChannelName of(String raw) {
        if (raw == null) {
            return new ChannelName(null);
        }
        String normalized = Guard.notBlank(raw, "channelName");
        String sanitized = Guard.maxLength(normalized, MAX_LENGTH, "channelName");
        return new ChannelName(sanitized);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChannelName that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
