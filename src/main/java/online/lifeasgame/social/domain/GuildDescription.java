package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class GuildDescription {

    @Column(name = "description_md", columnDefinition = "text")
    private String md;

    public static GuildDescription of(String md) {
        return new GuildDescription(md == null ? "" : md);
    }

    private GuildDescription(String md) {
        this.md = md;
    }
}
