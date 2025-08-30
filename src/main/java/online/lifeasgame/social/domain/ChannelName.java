package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelName {

    @Column(name = "name", length = 60)
    private String value;

    private ChannelName(String v) {
        this.value = v;
    }

    public static ChannelName of(String raw){
        if (raw == null) {
            return new ChannelName(null);
        }

        String trimmed = raw.strip();
        Guard.maxLength(trimmed, 60, "channelName");

        return new ChannelName(trimmed);
    }
    public String value(){ return value; }
}
