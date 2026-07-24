package online.lifeasgame.lifelog.domain.event;

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
    @DisplayName("Factory는 v1과 null primaryRoleId를 고정한다")
    void createsVersionOneWithoutPrimaryRole() {
        LifeLogRecorded event = LifeLogRecorded.of(
                "event-199",
                197L,
                199L,
                LifeLogType.COLLECTION,
                OCCURRED_AT
        );

        assertThat(event.eventId()).isEqualTo("event-199");
        assertThat(event.eventVersion())
                .isEqualTo(LifeLogRecorded.EVENT_VERSION);
        assertThat(event.playerId()).isEqualTo(197L);
        assertThat(event.lifeLogId()).isEqualTo(199L);
        assertThat(event.lifeLogType()).isEqualTo(LifeLogType.COLLECTION);
        assertThat(event.primaryRoleId()).isNull();
        assertThat(event.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    @DisplayName("필수 필드와 Event version을 검증한다")
    void validatesContract() {
        assertThatThrownBy(() -> new LifeLogRecorded(
                " ",
                1,
                197L,
                199L,
                LifeLogType.EXERCISE,
                null,
                OCCURRED_AT
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LifeLogRecorded(
                "event-199",
                2,
                197L,
                199L,
                LifeLogType.EXERCISE,
                null,
                OCCURRED_AT
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LifeLogRecorded(
                "event-199",
                1,
                null,
                199L,
                LifeLogType.EXERCISE,
                null,
                OCCURRED_AT
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LifeLogRecorded(
                "event-199",
                1,
                197L,
                null,
                LifeLogType.EXERCISE,
                null,
                OCCURRED_AT
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LifeLogRecorded(
                "event-199",
                1,
                197L,
                199L,
                null,
                null,
                OCCURRED_AT
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LifeLogRecorded(
                "event-199",
                1,
                197L,
                199L,
                LifeLogType.EXERCISE,
                null,
                null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Role Domain 전에는 primaryRoleId 입력을 거부한다")
    void rejectsPrimaryRoleId() {
        assertThatThrownBy(() -> new LifeLogRecorded(
                "event-199",
                1,
                197L,
                199L,
                LifeLogType.MEDIA,
                31L,
                OCCURRED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
