package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class PartyBanner {
    @Column(name = "banner_image_url", length = 512)
    private String imageUrl;
    @Column(name = "banner_bg_color", length = 16)
    private String bgColor;

    public static PartyBanner of(String imageUrl, String bgColor) {
        return new PartyBanner(imageUrl, bgColor);
    }

    private PartyBanner(String imageUrl, String bgColor) {
        this.imageUrl = imageUrl;
        this.bgColor = bgColor;
    }
}
