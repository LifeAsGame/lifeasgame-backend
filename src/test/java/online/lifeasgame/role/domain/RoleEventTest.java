package online.lifeasgame.role.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.error.RoleError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoleEvent domain")
class RoleEventTest {

    private static final Instant START =
            Instant.parse("2026-08-11T01:00:00Z");
    private static final Instant END = START.plusSeconds(3600);

    @Nested
    @DisplayName("Role 안에 사건 구조를 만들 때")
    class Create {

        @Test
        @DisplayName("PLANNED 상태와 명시한 시간 범위를 보존한다")
        void createsPlannedEvent() {
            RoleEvent event = event();

            assertThat(event.getPlayerId()).isEqualTo(1L);
            assertThat(event.getRoleId()).isEqualTo(2L);
            assertThat(event.getTitle()).isEqualTo("팀 회고");
            assertThat(event.getStartsAt()).isEqualTo(START);
            assertThat(event.getEndsAt()).isEqualTo(END);
            assertThat(event.getStatus()).isEqualTo(RoleEventStatus.PLANNED);
            assertThat(event.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("blank 또는 120자를 넘는 제목을 거부한다")
        void rejectsInvalidTitle() {
            assertError(
                    () -> create("   ", START, END),
                    RoleError.INVALID_ROLE_EVENT_TITLE
            );
            assertError(
                    () -> create("x".repeat(121), START, END),
                    RoleError.INVALID_ROLE_EVENT_TITLE
            );
        }

        @Test
        @DisplayName("종료 시각이 시작 시각보다 빠르면 거부한다")
        void rejectsInvalidTimeRange() {
            assertError(
                    () -> create("팀 회고", END, START),
                    RoleError.INVALID_ROLE_EVENT_TIME_RANGE
            );
        }
    }

    @Nested
    @DisplayName("PLANNED 사건을 변경할 때")
    class ChangePlannedEvent {

        @Test
        @DisplayName("구조를 수정하고 명시적 완료 시각으로 COMPLETED가 된다")
        void updatesAndCompletes() {
            RoleEvent event = event();
            Instant completedAt = END.plusSeconds(10);

            event.update("새 제목", null, null, null);
            event.complete(completedAt);

            assertThat(event.getTitle()).isEqualTo("새 제목");
            assertThat(event.getStartsAt()).isNull();
            assertThat(event.getStatus()).isEqualTo(RoleEventStatus.COMPLETED);
            assertThat(event.getCompletedAt()).isEqualTo(completedAt);
        }

        @Test
        @DisplayName("취소하면 CANCELED가 되고 완료 시각은 생기지 않는다")
        void cancels() {
            RoleEvent event = event();

            event.cancel();

            assertThat(event.getStatus()).isEqualTo(RoleEventStatus.CANCELED);
            assertThat(event.getCompletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("terminal 사건을 다시 변경할 때")
    class TerminalTransitions {

        @Test
        @DisplayName("COMPLETED는 수정·취소·재완료할 수 없다")
        void rejectsCompletedTransitions() {
            RoleEvent event = event();
            event.complete(END);

            assertNotPlanned(() -> event.update("변경", null, null, null));
            assertNotPlanned(event::cancel);
            assertNotPlanned(() -> event.complete(END.plusSeconds(1)));
        }

        @Test
        @DisplayName("CANCELED는 수정·완료할 수 없다")
        void rejectsCanceledTransitions() {
            RoleEvent event = event();
            event.cancel();

            assertNotPlanned(() -> event.update("변경", null, null, null));
            assertNotPlanned(() -> event.complete(END));
        }
    }

    @Nested
    @DisplayName("참여자를 구분해 추가할 때")
    class Participants {

        @Test
        @DisplayName("같은 ID의 PERSON과 SERVICE_USER를 별도 참여자로 유지한다")
        void keepsParticipantMeaningsSeparate() {
            RoleEvent event = event();

            event.addParticipant(RoleEventParticipantType.PERSON, 3L);
            event.addParticipant(RoleEventParticipantType.SERVICE_USER, 3L);

            assertThat(event.getParticipants())
                    .extracting(RoleEventParticipant::getParticipantType)
                    .containsExactly(
                            RoleEventParticipantType.PERSON,
                            RoleEventParticipantType.SERVICE_USER
                    );
        }

        @Test
        @DisplayName("동일 type과 ID 중복은 409 domain error로 거부한다")
        void rejectsDuplicateParticipant() {
            RoleEvent event = event();
            event.addParticipant(RoleEventParticipantType.PERSON, 3L);

            assertError(
                    () -> event.addParticipant(
                            RoleEventParticipantType.PERSON,
                            3L
                    ),
                    RoleError.ROLE_EVENT_PARTICIPANT_ALREADY_EXISTS
            );
        }
    }

    private RoleEvent event() {
        return create("  팀 회고  ", START, END);
    }

    private RoleEvent create(
            String title,
            Instant startsAt,
            Instant endsAt
    ) {
        return RoleEvent.create(
                1L,
                2L,
                title,
                " 회고를 나눈다 ",
                startsAt,
                endsAt
        );
    }

    private void assertNotPlanned(Runnable action) {
        assertError(action, RoleError.ROLE_EVENT_NOT_PLANNED);
    }

    private void assertError(Runnable action, RoleError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
