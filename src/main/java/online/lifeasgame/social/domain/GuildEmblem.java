package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class GuildEmblem {

    @Column(name = "emblem_image_url", length = 512)
    private String imageUrl;

    @Column(name = "emblem_bg_color", length = 16)
    private String bgColor;

    public static GuildEmblem of(String imageUrl, String bgColor) {
        return new GuildEmblem(imageUrl, bgColor);
    }

    private GuildEmblem(String imageUrl, String bgColor) {
        this.imageUrl = imageUrl;
        this.bgColor = bgColor;
    }
}
