package online.lifeasgame.character.application.view;

import online.lifeasgame.character.domain.TitleCategory;

import java.time.Instant;

public interface PlayerTitleView {

    Long getTitleId();

    String getCode();

    String getName();

    TitleCategory getCategory();

    String getDescMd();

    Instant getAcquiredAt();
}
