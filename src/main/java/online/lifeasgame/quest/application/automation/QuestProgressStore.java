package online.lifeasgame.quest.application.automation;

import online.lifeasgame.quest.domain.QuestCode;

import java.time.Duration;

public interface QuestProgressStore {

    int increment(QuestCode questCode, Long playerId, int delta, Duration ttl);

    int set(QuestCode questCode, Long playerId, int value, Duration ttl);

    void reset(QuestCode questCode, Long playerId);
}
