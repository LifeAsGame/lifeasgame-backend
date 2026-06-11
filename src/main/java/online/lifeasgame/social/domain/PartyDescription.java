package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class PartyDescription {
    @Column(name = "description_md", columnDefinition = "text")
    private String md;

    public static PartyDescription of(String md) {
        return new PartyDescription(md == null ? "" : md);
    }

    private PartyDescription(String md) {
        this.md = md;
    }
}
