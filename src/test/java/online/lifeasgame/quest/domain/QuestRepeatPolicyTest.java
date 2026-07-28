package online.lifeasgame.quest.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Quest repeat policy")
class QuestRepeatPolicyTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 7, 27);

    @Test
    @DisplayName("ONCE와 legacy NONE은 영구 기간을 사용한다")
    void usesForeverForOneTimePolicies() {
        assertPeriod(
                QuestRepeatRule.ONCE.periodFor(MONDAY),
                LocalDate.of(1970, 1, 1),
                LocalDate.of(9999, 12, 31)
        );
        assertPeriod(
                QuestRepeatRule.NONE.periodFor(MONDAY),
                LocalDate.of(1970, 1, 1),
                LocalDate.of(9999, 12, 31)
        );
        assertThat(QuestRepeatRule.ONCE.isOneTime()).isTrue();
        assertThat(QuestRepeatRule.NONE.isOneTime()).isTrue();
    }

    @Test
    @DisplayName("DAILY와 WEEKLY는 공식 기간 경계를 유지한다")
    void calculatesFinalRepeatPeriods() {
        assertPeriod(
                QuestRepeatRule.DAILY.periodFor(MONDAY),
                MONDAY,
                MONDAY
        );
        assertPeriod(
                QuestRepeatRule.WEEKLY.periodFor(MONDAY.plusDays(3)),
                MONDAY,
                MONDAY.plusDays(6)
        );
    }

    @Test
    @DisplayName("legacy MONTHLY는 월 기간 계산을 유지한다")
    void keepsLegacyMonthlyPeriod() {
        assertPeriod(
                QuestRepeatRule.MONTHLY.periodFor(MONDAY),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );
    }

    @Test
    @DisplayName("ONCE는 NONE의 idempotency TTL 의미를 유지한다")
    void keepsIdempotencyTtlCompatibility() {
        assertThat(QuestRepeatRule.ONCE.idempotencyTtl())
                .isEqualTo(Duration.ofDays(90));
        assertThat(QuestRepeatRule.NONE.idempotencyTtl())
                .isEqualTo(Duration.ofDays(90));
        assertThat(QuestRepeatRule.DAILY.idempotencyTtl())
                .isEqualTo(Duration.ofDays(7));
        assertThat(QuestRepeatRule.WEEKLY.idempotencyTtl())
                .isEqualTo(Duration.ofDays(30));
        assertThat(QuestRepeatRule.MONTHLY.idempotencyTtl())
                .isEqualTo(Duration.ofDays(120));
    }

    private void assertPeriod(
            TimePeriod period,
            LocalDate expectedStart,
            LocalDate expectedEnd
    ) {
        assertThat(period.start()).isEqualTo(expectedStart);
        assertThat(period.end()).isEqualTo(expectedEnd);
    }
}
