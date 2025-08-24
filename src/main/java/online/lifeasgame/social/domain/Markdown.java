package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Markdown {

    @Lob
    @Column(name = "markdown", columnDefinition = "LONGTEXT")
    private String text;

    private Markdown(String text) {
        this.text = text == null ? "" : text;
    }

    public static Markdown of(String md) { return new Markdown(md); }
    public String text() { return text; }
}
