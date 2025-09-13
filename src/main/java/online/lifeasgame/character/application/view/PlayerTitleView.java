package online.lifeasgame.character.application.view;

import java.time.Instant;
import online.lifeasgame.character.domain.TitleCategory;

public interface PlayerTitleView {
    Long getTitleId();
    String getCode();
    String getName();
    TitleCategory getCategory();
    String getDescMd();
    Instant getAcquiredAt();
}
