package online.lifeasgame.lifelog.domain.event;

import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LifeLogRecorded 계약")
class LifeLogRecordedTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T09:00:00Z");

    @Test
    @DisplayName("Content-ready Fact는 공식 type/version과 Definition Snapshot을 보존한다")
    void createsContentReadyFact() {
        LifeLogRecorded event = contentReady();

        assertThat(event.eventId()).isEqualTo("event-213");
        assertThat(event.eventType())
                .isEqualTo(LifeLogRecorded.EVENT_TYPE);
        assertThat(event.eventVersion())
                .isEqualTo(LifeLogRecorded.EVENT_VERSION);
        assertThat(event.playerId()).isEqualTo(197L);
        assertThat(event.lifeLogId()).isEqualTo(213L);
        assertThat(event.sourceDefinitionVersion()).isEqualTo(1);
        assertThat(event.subtype()).isEqualTo(LifeLogSubtype.ACTIVITY);
        assertThat(event.entryMode()).isEqualTo(LifeLogEntryMode.FULL);
        assertThat(event.primaryRoleId()).isNull();
        assertThat(event.legacyLifeLogType()).isNull();
        assertThat(event.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.isContentReady()).isTrue();
        assertThat(event.requireContentReady()).isSameAs(event);
    }

    @Test
    @DisplayName("metadata 없는 신규 legacy create Fact는 Content consumer 사용을 거부한다")
    void marksNewLegacyCreateAsNotContentReady() {
        LifeLogRecorded event = new LifeLogRecorded(
                "event-legacy-create",
                LifeLogRecorded.EVENT_TYPE,
                1,
                OCCURRED_AT,
                197L,
                214L,
                1,
                null,
                LifeLogEntryMode.FULL,
                null,
                null,
                null,
                null
        );

        assertThat(event.isContentReady()).isFalse();
        assertThatThrownBy(event::requireContentReady)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("과거 transport Fact는 physical type을 decode용으로만 보존한다")
    void supportsLegacyTransportFact() {
        LifeLogRecorded event = LifeLogRecorded.legacy(
                "legacy-event",
                1,
                197L,
                51L,
                LifeLogType.EXERCISE,
                null,
                OCCURRED_AT
        );

        assertThat(event.legacyLifeLogType())
                .isEqualTo(LifeLogType.EXERCISE);
        assertThat(event.sourceDefinitionVersion()).isNull();
        assertThat(event.entryMode()).isNull();
        assertThat(event.isContentReady()).isFalse();
    }

    @Test
    @DisplayName("eventType, version, Definition version과 primaryRole 계약을 검증한다")
    void validatesFactContract() {
        assertThatThrownBy(() -> withContract(
                "OtherEvent",
                1,
                1,
                null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withContract(
                LifeLogRecorded.EVENT_TYPE,
                2,
                1,
                null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withContract(
                LifeLogRecorded.EVENT_TYPE,
                1,
                0,
                null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withContract(
                LifeLogRecorded.EVENT_TYPE,
                1,
                1,
                31L
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("weekly reflection은 periodKey pairing을 요구한다")
    void validatesReflectionPairing() {
        assertThatThrownBy(() -> new LifeLogRecorded(
                "event-weekly",
                LifeLogRecorded.EVENT_TYPE,
                1,
                OCCURRED_AT,
                197L,
                213L,
                1,
                LifeLogSubtype.REFLECTION,
                LifeLogEntryMode.QUICK,
                LifeLogReflectionScope.WEEKLY_LOOKBACK,
                null,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private LifeLogRecorded contentReady() {
        return new LifeLogRecorded(
                "event-213",
                LifeLogRecorded.EVENT_TYPE,
                1,
                OCCURRED_AT,
                197L,
                213L,
                1,
                LifeLogSubtype.ACTIVITY,
                LifeLogEntryMode.FULL,
                null,
                null,
                null,
                null
        );
    }

    private LifeLogRecorded withContract(
            String eventType,
            int eventVersion,
            int sourceDefinitionVersion,
            Long primaryRoleId
    ) {
        return new LifeLogRecorded(
                "event-213",
                eventType,
                eventVersion,
                OCCURRED_AT,
                197L,
                213L,
                sourceDefinitionVersion,
                LifeLogSubtype.ACTIVITY,
                LifeLogEntryMode.FULL,
                null,
                null,
                primaryRoleId,
                null
        );
    }
}
