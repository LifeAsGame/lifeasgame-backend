package online.lifeasgame.platform.outbox.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OutboxEvent")
class OutboxEventTest {

    private static final Instant NOW =
            Instant.parse("2026-07-24T06:00:00Z");

    @Nested
    @DisplayName("전달 대상을 claim할 때")
    class Claim {

        @Test
        @DisplayName("PENDING을 PROCESSING으로 바꾸고 lease 소유자를 기록한다")
        void claimsPendingEvent() {
            OutboxEvent event = pending();

            event.claim("relay-a", NOW);

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
            assertThat(event.getLockedAt()).isEqualTo(NOW);
            assertThat(event.getLockedBy()).isEqualTo("relay-a");
        }

        @Test
        @DisplayName("PUBLISHED Event는 다시 claim할 수 없다")
        void rejectsPublishedEvent() {
            OutboxEvent event = pending();
            event.claim("relay-a", NOW);
            event.markPublished("relay-a", NOW.plusSeconds(1));

            assertOutboxError(
                    () -> event.claim("relay-a", NOW.plusSeconds(2)),
                    OutboxError.OUTBOX_EVENT_STATE_INVALID
            );
        }
    }

    @Nested
    @DisplayName("전달 결과를 반영할 때")
    class CompleteOrFail {

        @Test
        @DisplayName("성공하면 PUBLISHED로 바꾸고 lease를 제거한다")
        void marksPublished() {
            OutboxEvent event = pending();
            event.claim("relay-a", NOW);

            event.markPublished("relay-a", NOW.plusSeconds(1));

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(event.getPublishedAt()).isEqualTo(NOW.plusSeconds(1));
            assertThat(event.getLockedAt()).isNull();
            assertThat(event.getLockedBy()).isNull();
        }

        @Test
        @DisplayName("최대 횟수 전 실패는 PENDING과 다음 시각을 기록한다")
        void schedulesRetry() {
            OutboxEvent event = pending();
            event.claim("relay-a", NOW);

            event.markFailed(
                    "relay-a",
                    2,
                    NOW.plusSeconds(1),
                    NOW.plusSeconds(5),
                    "safe error"
            );

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(event.getAttemptCount()).isEqualTo(1);
            assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
            assertThat(event.getLastError()).isEqualTo("safe error");
            assertThat(event.getLockedBy()).isNull();
        }

        @Test
        @DisplayName("최대 횟수 실패는 FAILED로 종료한다")
        void marksFailedAtMaxAttempts() {
            OutboxEvent event = pending();
            event.claim("relay-a", NOW);
            event.markFailed(
                    "relay-a",
                    2,
                    NOW.plusSeconds(1),
                    NOW.plusSeconds(1),
                    "first"
            );
            event.claim("relay-a", NOW.plusSeconds(1));

            event.markFailed(
                    "relay-a",
                    2,
                    NOW.plusSeconds(2),
                    NOW.plusSeconds(2),
                    "second"
            );

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(event.getAttemptCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("다른 relay 소유자는 완료할 수 없다")
        void rejectsDifferentOwner() {
            OutboxEvent event = pending();
            event.claim("relay-a", NOW);

            assertOutboxError(
                    () -> event.markPublished(
                            "relay-b",
                            NOW.plusSeconds(1)
                    ),
                    OutboxError.OUTBOX_EVENT_LOCK_OWNER_MISMATCH
            );
        }
    }

    @Nested
    @DisplayName("만료된 lease를 복구할 때")
    class RecoverLease {

        @Test
        @DisplayName("PROCESSING을 즉시 처리 가능한 PENDING으로 되돌린다")
        void recoversProcessingEvent() {
            OutboxEvent event = pending();
            event.claim("relay-a", NOW);

            event.recoverExpiredLease(NOW.plusSeconds(31));

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(event.getNextAttemptAt())
                    .isEqualTo(NOW.plusSeconds(31));
            assertThat(event.getLockedAt()).isNull();
            assertThat(event.getLockedBy()).isNull();
        }
    }

    private OutboxEvent pending() {
        return OutboxEvent.pending(
                "00000000-0000-0000-0000-000000000197",
                "player.registered.v1",
                "{\"playerId\":197}",
                NOW,
                NOW
        );
    }

    private void assertOutboxError(Runnable action, OutboxError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
