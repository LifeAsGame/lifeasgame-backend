package online.lifeasgame.lifelog.domain.record;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("canonical LifeLogRecord 계약")
class LifeLogRecordTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-29T15:30:00Z");

    @Test
    @DisplayName("공식 subtype만 exact value로 parse한다")
    void parsesOfficialSubtypeStrictly() {
        assertThat(LifeLogSubtype.parse("QUICK_NOTE"))
                .isEqualTo(LifeLogSubtype.QUICK_NOTE);
        assertThat(LifeLogSubtype.parse("HEALTH_NOTE"))
                .isEqualTo(LifeLogSubtype.HEALTH_NOTE);

        assertThatThrownBy(() -> LifeLogSubtype.parse("COLLECTION"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LifeLogSubtype.parse("activity"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LifeLogSubtype.parse(" ACTIVITY "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("legacy factory는 subtype null과 FULL/QUICK을 허용한다")
    void createsLegacyRecordWithoutSubtype() {
        LifeLogRecord full = LifeLogRecord.legacy(
                1L,
                LifeLogSourceType.COLLECTION,
                1L,
                LifeLogEntryMode.FULL,
                OCCURRED_AT
        );
        LifeLogRecord quick = LifeLogRecord.legacy(
                1L,
                LifeLogSourceType.MEDIA,
                1L,
                LifeLogEntryMode.QUICK,
                OCCURRED_AT
        );

        assertThat(full.getSourceDefinitionVersion()).isEqualTo(1);
        assertThat(full.getSubtype()).isNull();
        assertThat(full.isContentReady()).isFalse();
        assertThat(quick.getEntryMode()).isEqualTo(LifeLogEntryMode.QUICK);
        assertThat(quick.getPrimaryRoleId()).isNull();
    }

    @Test
    @DisplayName("content-ready factory는 subtype을 필수로 요구한다")
    void requiresContentReadySubtype() {
        assertThatThrownBy(() -> LifeLogRecord.contentReady(
                1L,
                LifeLogSourceType.EXERCISE,
                1L,
                null,
                LifeLogEntryMode.FULL,
                null,
                null,
                OCCURRED_AT
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("weekly reflection pairing과 primaryRole null 계약을 보존한다")
    void validatesReflectionAndRoleContract() {
        LifeLogPeriodKey periodKey =
                new LifeLogPeriodKey("2026-W31");
        LifeLogRecord record = LifeLogRecord.contentReady(
                1L,
                LifeLogSourceType.COLLECTION,
                1L,
                LifeLogSubtype.REFLECTION,
                LifeLogEntryMode.FULL,
                LifeLogReflectionScope.WEEKLY_LOOKBACK,
                periodKey,
                OCCURRED_AT
        );

        assertThat(record.getPeriodKey()).isEqualTo("2026-W31");
        assertThat(record.getPrimaryRoleId()).isNull();
        assertThatThrownBy(() -> LifeLogRecord.contentReady(
                1L,
                LifeLogSourceType.COLLECTION,
                1L,
                LifeLogSubtype.ACTIVITY,
                LifeLogEntryMode.FULL,
                LifeLogReflectionScope.WEEKLY_LOOKBACK,
                periodKey,
                OCCURRED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Asia/Seoul 연말연초도 ISO week-based year로 계산한다")
    void calculatesIsoWeeklyPeriodAtYearBoundary() {
        ZoneId seoul = ZoneId.of("Asia/Seoul");

        assertThat(LifeLogPeriodKey.weekly(
                Instant.parse("2026-12-31T15:30:00Z"),
                seoul
        ).value()).isEqualTo("2026-W53");
        assertThat(LifeLogPeriodKey.weekly(
                Instant.parse("2027-01-03T14:59:59Z"),
                seoul
        ).value()).isEqualTo("2026-W53");
        assertThat(LifeLogPeriodKey.weekly(
                Instant.parse("2027-01-03T15:00:00Z"),
                seoul
        ).value()).isEqualTo("2027-W01");
    }

    @Test
    @DisplayName("periodKey는 weekly 형식과 범위를 검증한다")
    void validatesPeriodKey() {
        assertThatThrownBy(() -> new LifeLogPeriodKey("2026-31"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LifeLogPeriodKey("2026-W00"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LifeLogPeriodKey("2026-W54"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LifeLogPeriodKey("2025-W53"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
