package online.lifeasgame.role.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.role.application.command.RoleEventCommand;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleEvent;
import online.lifeasgame.role.domain.RoleEventParticipant;
import online.lifeasgame.role.domain.RoleType;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.user.application.internal.UserLookupApi;
import online.lifeasgame.user.domain.error.UserError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleEvent Application")
class RoleEventApplicationTest {

    private static final Long PLAYER_ID = 252L;
    private static final Long ROLE_ID = 25L;
    private static final Long EVENT_ID = 52L;
    private static final Instant NOW =
            Instant.parse("2026-08-11T03:00:00Z");

    @Mock
    private RoleReader roleReader;

    @Mock
    private RoleEventReader eventReader;

    @Mock
    private RoleEventWriter eventWriter;

    @Mock
    private PersonLookupApi personLookupApi;

    @Mock
    private UserLookupApi userLookupApi;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @Mock
    private Clock clock;

    @InjectMocks
    private RoleEventService service;

    @BeforeEach
    void setUp() {
        lenient().when(currentPlayerAccessor.currentPlayerIdOrThrow())
                .thenReturn(PLAYER_ID);
        lenient().when(eventWriter.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("RoleEvent를 생성할 때")
    class Create {

        @Test
        @DisplayName("현재 Player가 소유한 ACTIVE Role에만 생성한다")
        void createsForOwnedActiveRole() {
            given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID))
                    .willReturn(role());

            var result = service.create(ROLE_ID, createCommand());

            assertThat(result.roleId()).isEqualTo(ROLE_ID);
            assertThat(result.status()).isEqualTo("PLANNED");
            verify(roleReader).getOwnedForUpdate(ROLE_ID, PLAYER_ID);
        }

        @Test
        @DisplayName("ARCHIVED Role에는 생성하지 않는다")
        void rejectsArchivedRole() {
            Role role = role();
            role.archive();
            given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID))
                    .willReturn(role);

            assertRoleError(
                    () -> service.create(ROLE_ID, createCommand()),
                    RoleError.ROLE_ARCHIVED
            );
        }
    }

    @Nested
    @DisplayName("RoleEvent 상태를 변경할 때")
    class ChangeStatus {

        @Test
        @DisplayName("Clock의 명시적 시각으로 완료한다")
        void completesAtClockInstant() {
            RoleEvent event = event();
            given(eventReader.getOwnedForUpdate(
                    EVENT_ID,
                    ROLE_ID,
                    PLAYER_ID
            )).willReturn(event);
            given(clock.instant()).willReturn(NOW);

            var result = service.complete(ROLE_ID, EVENT_ID);

            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.completedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("다른 Player의 Event는 not found로 거부한다")
        void rejectsAnotherPlayersEvent() {
            given(eventReader.getOwnedForUpdate(
                    EVENT_ID,
                    ROLE_ID,
                    PLAYER_ID
            )).willThrow(new DomainException(
                    RoleError.ROLE_EVENT_NOT_FOUND
            ));

            assertRoleError(
                    () -> service.cancel(ROLE_ID, EVENT_ID),
                    RoleError.ROLE_EVENT_NOT_FOUND
            );
        }

        @Test
        @DisplayName("terminal Event의 구조 수정은 거부한다")
        void rejectsUpdatingTerminalEvent() {
            RoleEvent event = event();
            event.complete(NOW);
            given(eventReader.getOwnedForUpdate(
                    EVENT_ID,
                    ROLE_ID,
                    PLAYER_ID
            )).willReturn(event);

            assertRoleError(
                    () -> service.update(
                            ROLE_ID,
                            EVENT_ID,
                            new RoleEventCommand.Update(
                                    "변경",
                                    null,
                                    null,
                                    null
                            )
                    ),
                    RoleError.ROLE_EVENT_NOT_PLANNED
            );
        }
    }

    @Nested
    @DisplayName("RoleEvent 참여자를 추가할 때")
    class AddParticipant {

        @Test
        @DisplayName("linkedUserId가 있어도 PERSON으로 유지한다")
        void preservesPersonMeaning() {
            RoleEvent event = eventForUpdate();
            given(personLookupApi.getOwnedActive(3L, PLAYER_ID))
                    .willReturn(new PersonLookupApi.PersonReference(
                            3L,
                            30L,
                            "Alice"
                    ));

            var result = service.addParticipant(
                    ROLE_ID,
                    EVENT_ID,
                    new RoleEventCommand.AddParticipant("PERSON", 3L)
            );

            assertThat(result.participantType()).isEqualTo("PERSON");
            assertThat(event.getParticipants()).singleElement()
                    .extracting(RoleEventParticipant::getParticipantId)
                    .isEqualTo(3L);
            verifyNoInteractions(userLookupApi);
        }

        @Test
        @DisplayName("다른 Player 또는 archived Person 오류를 그대로 전파한다")
        void rejectsInvalidPerson() {
            eventForUpdate();
            given(personLookupApi.getOwnedActive(3L, PLAYER_ID))
                    .willThrow(new DomainException(PersonError.PERSON_NOT_FOUND));

            assertThatThrownBy(() -> service.addParticipant(
                    ROLE_ID,
                    EVENT_ID,
                    new RoleEventCommand.AddParticipant("PERSON", 3L)
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(PersonError.PERSON_NOT_FOUND)
            );
        }

        @Test
        @DisplayName("ACTIVE SERVICE_USER만 별도 type으로 추가한다")
        void addsActiveServiceUser() {
            eventForUpdate();
            given(userLookupApi.getActive(7L)).willReturn(
                    new UserLookupApi.UserReference(7L, "service-user")
            );

            var result = service.addParticipant(
                    ROLE_ID,
                    EVENT_ID,
                    new RoleEventCommand.AddParticipant("SERVICE_USER", 7L)
            );

            assertThat(result.participantType()).isEqualTo("SERVICE_USER");
            verifyNoInteractions(personLookupApi);
        }

        @Test
        @DisplayName("inactive SERVICE_USER 오류를 전파한다")
        void rejectsInactiveServiceUser() {
            eventForUpdate();
            given(userLookupApi.getActive(7L)).willThrow(
                    new DomainException(UserError.USER_NOT_ACTIVE)
            );

            assertThatThrownBy(() -> service.addParticipant(
                    ROLE_ID,
                    EVENT_ID,
                    new RoleEventCommand.AddParticipant("SERVICE_USER", 7L)
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(UserError.USER_NOT_ACTIVE)
            );
        }
    }

    @Nested
    @DisplayName("참여자 link를 제거할 때")
    class RemoveParticipant {

        @Test
        @DisplayName("path의 owned Event 안에 있는 link만 제거한다")
        void removesOnlyOwnedEventLink() {
            RoleEvent event = event();
            RoleEventParticipant participant = event.addParticipant(
                    online.lifeasgame.role.domain.RoleEventParticipantType.PERSON,
                    3L
            );
            ReflectionTestUtils.setField(participant, "id", 9L);
            given(eventReader.getOwnedForUpdate(
                    EVENT_ID,
                    ROLE_ID,
                    PLAYER_ID
            )).willReturn(event);

            service.removeParticipant(ROLE_ID, EVENT_ID, 9L);

            assertThat(event.getParticipants()).isEmpty();
        }

        @Test
        @DisplayName("path의 Event에 속하지 않은 link는 삭제하지 않는다")
        void rejectsAnotherEventsLink() {
            eventForUpdate();

            assertRoleError(
                    () -> service.removeParticipant(
                            ROLE_ID,
                            EVENT_ID,
                            9L
                    ),
                    RoleError.ROLE_EVENT_PARTICIPANT_NOT_FOUND
            );
        }
    }

    @Test
    @DisplayName("RoleEventService는 LifeLog 등록 dependency를 갖지 않는다")
    void hasNoAutomaticLifeLogDependency() {
        assertThat(Arrays.stream(RoleEventService.class.getDeclaredFields())
                .map(field -> field.getType().getName()))
                .doesNotContain(LifeLogRecordRegistrar.class.getName());
    }

    private Role role() {
        Role role = Role.create(
                PLAYER_ID,
                RoleType.of("WORK"),
                "Developer",
                null
        );
        ReflectionTestUtils.setField(role, "id", ROLE_ID);
        return role;
    }

    private RoleEvent event() {
        RoleEvent event = RoleEvent.create(
                PLAYER_ID,
                ROLE_ID,
                "팀 회고",
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        return event;
    }

    private RoleEvent eventForUpdate() {
        RoleEvent event = event();
        given(eventReader.getOwnedForUpdate(
                EVENT_ID,
                ROLE_ID,
                PLAYER_ID
        )).willReturn(event);
        return event;
    }

    private RoleEventCommand.Create createCommand() {
        return new RoleEventCommand.Create(
                "팀 회고",
                null,
                null,
                null
        );
    }

    private void assertRoleError(Runnable action, RoleError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
