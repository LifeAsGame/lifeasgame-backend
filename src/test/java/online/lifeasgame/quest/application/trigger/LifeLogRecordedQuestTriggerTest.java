package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.application.automation.QuestSignalAcceptancePolicy;
import online.lifeasgame.quest.application.automation.QuestSignalFingerprint;
import online.lifeasgame.quest.domain.QuestCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LifeLogRecorded Quest Trigger")
class LifeLogRecordedQuestTriggerTest {

    private static final Long PLAYER_ID = 215L;
    private static final Long LIFE_LOG_ID = 2150L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-30T02:00:00Z");

    private final LifeLogRecordedQuestTrigger trigger =
            new LifeLogRecordedQuestTrigger();

    @Test
    @DisplayName("모든 공식 content-ready subtype과 FULL/QUICK을 첫/세 기록 Signal로 번역한다")
    void translatesAllOfficialContentReadyRecords() {
        for (LifeLogSubtype subtype : LifeLogSubtype.values()) {
            for (LifeLogEntryMode entryMode : LifeLogEntryMode.values()) {
                List<QuestSignal> signals = trigger.translate(
                        event(
                                "event-" + subtype + "-" + entryMode,
                                subtype,
                                entryMode,
                                null,
                                null
                        )
                );

                assertThat(signals)
                        .extracting(QuestSignal::questCode)
                        .containsExactly(
                                QuestCode.Q_RECORD_FIRST_TRACE,
                                QuestCode.Q_RECORD_THREE_TRACES
                        );
                assertThat(signals).allSatisfy(signal -> {
                    assertThat(signal.acceptancePolicy())
                            .isEqualTo(
                                    QuestSignalAcceptancePolicy.EXISTING_ONLY
                            );
                    assertThat(signal.correlationId())
                            .isEqualTo("lifelog:" + LIFE_LOG_ID);
                    assertThat(signal.progressDelta()).isEqualTo(1);
                    assertThat(signal.periodKey()).isNull();
                    assertThat(signal.attributes())
                            .containsKeys(
                                    "lifeLogId",
                                    "sourceDefinitionVersion",
                                    "subtype",
                                    "entryMode",
                                    "reflectionScope",
                                    "periodKey",
                                    "primaryRoleId"
                            )
                            .containsEntry("reflectionScope", null)
                            .containsEntry("periodKey", null)
                            .containsEntry("primaryRoleId", null);
                });
            }
        }
    }

    @Test
    @DisplayName("FULL REFLECTION WEEKLY_LOOKBACK만 exact period Signal을 추가한다")
    void translatesOnlyFullWeeklyLookback() {
        LifeLogRecorded full = event(
                "weekly-full",
                LifeLogSubtype.REFLECTION,
                LifeLogEntryMode.FULL,
                LifeLogReflectionScope.WEEKLY_LOOKBACK,
                "2026-W31"
        );
        LifeLogRecorded quick = event(
                "weekly-quick",
                LifeLogSubtype.REFLECTION,
                LifeLogEntryMode.QUICK,
                LifeLogReflectionScope.WEEKLY_LOOKBACK,
                "2026-W31"
        );

        List<QuestSignal> fullSignals = trigger.translate(full);
        List<QuestSignal> quickSignals = trigger.translate(quick);

        assertThat(fullSignals)
                .extracting(QuestSignal::questCode)
                .containsExactly(
                        QuestCode.Q_RECORD_FIRST_TRACE,
                        QuestCode.Q_RECORD_THREE_TRACES,
                        QuestCode.Q_RECORD_WEEKLY_LOOKBACK
                );
        QuestSignal weekly = fullSignals.getLast();
        assertThat(weekly.periodKey()).isEqualTo("2026-W31");
        assertThat(weekly.attributes())
                .containsEntry("lifeLogId", LIFE_LOG_ID)
                .containsEntry("sourceDefinitionVersion", 1)
                .containsEntry("subtype", "REFLECTION")
                .containsEntry("entryMode", "FULL")
                .containsEntry(
                        "reflectionScope",
                        "WEEKLY_LOOKBACK"
                )
                .containsEntry("periodKey", "2026-W31")
                .doesNotContainKeys(
                        "eventId",
                        "lifeLogType",
                        "sourceType",
                        "title",
                        "memo",
                        "content"
                );
        assertThat(quickSignals)
                .extracting(QuestSignal::questCode)
                .containsExactly(
                        QuestCode.Q_RECORD_FIRST_TRACE,
                        QuestCode.Q_RECORD_THREE_TRACES
                );
    }

    @Test
    @DisplayName("legacy와 subtype 없는 비 content-ready Fact는 무시한다")
    void ignoresLegacyAndIncompleteFacts() {
        LifeLogRecorded legacy = LifeLogRecorded.legacy(
                "legacy-event",
                LifeLogRecorded.EVENT_VERSION,
                PLAYER_ID,
                LIFE_LOG_ID,
                LifeLogType.EXERCISE,
                null,
                OCCURRED_AT
        );
        LifeLogRecorded incomplete = event(
                "incomplete-event",
                null,
                LifeLogEntryMode.FULL,
                null,
                null
        );

        assertThat(trigger.translate(legacy)).isEmpty();
        assertThat(trigger.translate(incomplete)).isEmpty();
    }

    @Test
    @DisplayName("같은 global lifeLogId는 eventId가 달라도 같은 semantic fingerprint다")
    void ignoresEventIdInSemanticIdentity() {
        QuestSignal first = trigger.translate(event(
                "event-a",
                LifeLogSubtype.STUDY,
                LifeLogEntryMode.FULL,
                null,
                null
        )).get(1);
        QuestSignal second = trigger.translate(event(
                "event-b",
                LifeLogSubtype.STUDY,
                LifeLogEntryMode.FULL,
                null,
                null
        )).get(1);
        QuestSignalFingerprint fingerprint = new QuestSignalFingerprint();

        assertThat(first.correlationId()).isEqualTo("lifelog:" + LIFE_LOG_ID);
        assertThat(second.correlationId()).isEqualTo(first.correlationId());
        assertThat(fingerprint.fingerprint(second))
                .isEqualTo(fingerprint.fingerprint(first));
    }

    private LifeLogRecorded event(
            String eventId,
            LifeLogSubtype subtype,
            LifeLogEntryMode entryMode,
            LifeLogReflectionScope reflectionScope,
            String periodKey
    ) {
        return new LifeLogRecorded(
                eventId,
                LifeLogRecorded.EVENT_TYPE,
                LifeLogRecorded.EVENT_VERSION,
                OCCURRED_AT,
                PLAYER_ID,
                LIFE_LOG_ID,
                1,
                subtype,
                entryMode,
                reflectionScope,
                periodKey,
                null,
                null
        );
    }
}
