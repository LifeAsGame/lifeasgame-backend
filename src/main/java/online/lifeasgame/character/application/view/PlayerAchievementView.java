package online.lifeasgame.character.application.view;

import java.time.Instant;
import online.lifeasgame.character.domain.AchievementCategory;

public interface PlayerAchievementView {
    Long getAchievementId();
    String getCode();
    String getName();
    AchievementCategory getCategory();
    String getDescMd();
    Instant getAcquiredAt();
}
