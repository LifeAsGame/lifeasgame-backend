package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.character.domain.event.PlayerLeveledUp;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.domain.QuestCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PlayerLeveledUpQuestTrigger implements QuestTrigger<PlayerLeveledUp> {

    private static final List<LevelMilestone> MILESTONES = List.of(
            milestone(10, QuestCode.PLAYER_LEVEL_MILESTONE_10),
            milestone(20, QuestCode.PLAYER_LEVEL_MILESTONE_20),
            milestone(30, QuestCode.PLAYER_LEVEL_MILESTONE_30),
            milestone(40, QuestCode.PLAYER_LEVEL_MILESTONE_40),
            milestone(50, QuestCode.PLAYER_LEVEL_MILESTONE_50),
            milestone(60, QuestCode.PLAYER_LEVEL_MILESTONE_60),
            milestone(70, QuestCode.PLAYER_LEVEL_MILESTONE_70),
            milestone(80, QuestCode.PLAYER_LEVEL_MILESTONE_80),
            milestone(90, QuestCode.PLAYER_LEVEL_MILESTONE_90),
            milestone(100, QuestCode.PLAYER_LEVEL_MILESTONE_100)
    );

    @Override
    public Class<PlayerLeveledUp> eventType() {
        return PlayerLeveledUp.class;
    }

    @Override
    public List<QuestSignal> translate(PlayerLeveledUp event) {
        List<QuestSignal> signals = new ArrayList<>();
        int levelDelta = Math.max(1, event.afterLevel() - event.beforeLevel());
        String correlation = "player:" + event.playerId() + ":level-up:" + event.afterLevel();

        signals.add(
                QuestSignal.addProgress(QuestCode.PLAYER_LEVEL_TRACK, event.playerId(), levelDelta)
                        .occurredAt(event.occurredAt())
                        .correlationId(correlation)
                        .attribute("beforeLevel", event.beforeLevel())
                        .attribute("afterLevel", event.afterLevel())
                        .attribute("levelDelta", levelDelta)
                        .build()
        );

        for (LevelMilestone milestone : MILESTONES) {
            if (crossed(event.beforeLevel(), event.afterLevel(), milestone.threshold())) {
                signals.add(
                        QuestSignal.addProgress(milestone.code(), event.playerId(), 1)
                                .occurredAt(event.occurredAt())
                                .correlationId("player:" + event.playerId() + ":level-milestone:" + milestone.threshold())
                                .attribute("beforeLevel", event.beforeLevel())
                                .attribute("afterLevel", event.afterLevel())
                                .attribute("milestone", milestone.threshold())
                                .build()
                );
            }
        }

        return List.copyOf(signals);
    }

    private boolean crossed(int before, int after, int threshold) {
        return before < threshold && after >= threshold;
    }

    private static LevelMilestone milestone(int threshold, QuestCode questCode) {
        return new LevelMilestone(threshold, questCode);
    }

    private record LevelMilestone(int threshold, QuestCode code) {
    }
}
