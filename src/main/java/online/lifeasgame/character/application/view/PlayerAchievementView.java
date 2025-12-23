package online.lifeasgame.character.application.view;

import online.lifeasgame.character.domain.AchievementCategory;

import java.time.Instant;

public interface PlayerAchievementView {

    Long getAchievementId();

    String getCode();

    String getName();

    AchievementCategory getCategory();

    String getDescMd();

    Instant getAcquiredAt();
}
