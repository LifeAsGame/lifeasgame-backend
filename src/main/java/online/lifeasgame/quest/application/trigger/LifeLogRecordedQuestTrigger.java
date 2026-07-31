package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.application.automation.QuestSignalAcceptancePolicy;
import online.lifeasgame.quest.domain.QuestCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LifeLogRecordedQuestTrigger
        implements QuestTrigger<LifeLogRecorded> {

    @Override
    public Class<LifeLogRecorded> eventType() {
        return LifeLogRecorded.class;
    }

    @Override
    public List<QuestSignal> translate(LifeLogRecorded event) {
        if (!event.isContentReady()) {
            return List.of();
        }

        List<QuestSignal> signals = new ArrayList<>(3);
        signals.add(signal(QuestCode.Q_RECORD_FIRST_TRACE, event));
        signals.add(signal(QuestCode.Q_RECORD_THREE_TRACES, event));
        if (isWeeklyLookback(event)) {
            signals.add(signal(
                    QuestCode.Q_RECORD_WEEKLY_LOOKBACK,
                    event
            ));
        }
        return List.copyOf(signals);
    }

    private QuestSignal signal(
            QuestCode questCode,
            LifeLogRecorded event
    ) {
        return QuestSignal.addProgress(questCode, event.playerId(), 1)
                .occurredAt(event.occurredAt())
                .correlationId(
                        QuestSignalCorrelation.lifeLog(event.lifeLogId())
                )
                .acceptancePolicy(
                        QuestSignalAcceptancePolicy.EXISTING_ONLY
                )
                .periodKey(
                        questCode == QuestCode.Q_RECORD_WEEKLY_LOOKBACK
                                ? event.periodKey()
                                : null
                )
                .attribute("lifeLogId", event.lifeLogId())
                .attribute(
                        "sourceDefinitionVersion",
                        event.sourceDefinitionVersion()
                )
                .attribute("subtype", event.subtype().name())
                .attribute("entryMode", event.entryMode().name())
                .nullableAttribute(
                        "reflectionScope",
                        name(event.reflectionScope())
                )
                .nullableAttribute("periodKey", event.periodKey())
                .nullableAttribute(
                        "primaryRoleId",
                        event.primaryRoleId()
                )
                .build();
    }

    private boolean isWeeklyLookback(LifeLogRecorded event) {
        return event.entryMode() == LifeLogEntryMode.FULL
                && event.subtype() == LifeLogSubtype.REFLECTION
                && event.reflectionScope()
                == LifeLogReflectionScope.WEEKLY_LOOKBACK
                && event.periodKey() != null;
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
