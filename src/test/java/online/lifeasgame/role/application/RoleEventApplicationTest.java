package online.lifeasgame.role.application;

import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.role.application.command.RoleEventCommand;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleEvent;
import online.lifeasgame.role.domain.RoleEventParticipantType;
import online.lifeasgame.role.domain.RoleType;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.user.application.internal.UserLookupApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleEvent 명령의 소유권과 활성 Role 계약")
class RoleEventApplicationTest {

    private static final Long PLAYER_ID = 252L;
    private static final Long ROLE_ID = 25L;
    private static final Long EVENT_ID = 52L;

    @Mock private RoleReader roleReader;
    @Mock private RoleEventReader eventReader;
    @Mock private RoleEventWriter eventWriter;
    @Mock private PersonLookupApi personLookupApi;
    @Mock private UserLookupApi userLookupApi;
    @Mock private CurrentPlayerAccessor currentPlayerAccessor;
    @Mock private Clock clock;
    @InjectMocks private RoleEventService service;

    enum Command { CREATE, UPDATE, COMPLETE, CANCEL, ADD_PARTICIPANT, REMOVE_PARTICIPANT }

    @ParameterizedTest
    @EnumSource(Command.class)
    @DisplayName("보관한 Role의 모든 명령은 저장 전에 409로 거부한다")
    void rejectsArchivedRole(Command command) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
        Role role = Role.create(PLAYER_ID, RoleType.of("WORK"), "Developer", null);
        role.archive();
        given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID)).willReturn(role);

        assertRoleError(command, RoleError.ROLE_ARCHIVED);
        verifyNoInteractions(eventReader, eventWriter, personLookupApi, userLookupApi, clock);
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    @DisplayName("자기 Role가 아니면 모든 명령은 대상 조회 전에 404로 거부한다")
    void preservesOwnership(Command command) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
        given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID))
                .willThrow(new DomainException(RoleError.ROLE_NOT_FOUND));

        assertRoleError(command, RoleError.ROLE_NOT_FOUND);
        verifyNoInteractions(eventReader, eventWriter, personLookupApi, userLookupApi, clock);
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    @DisplayName("인증이 없으면 저장소 조회 전에 거부한다")
    void preservesAuthentication(Command command) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willThrow(new AuthException(AuthError.UNAUTHORIZED));

        assertThatThrownBy(() -> execute(command)).isInstanceOfSatisfying(
                AuthException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(AuthError.UNAUTHORIZED));
        verifyNoInteractions(roleReader, eventReader, eventWriter, personLookupApi, userLookupApi, clock);
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    @DisplayName("활성 Role의 명령은 저장되며 완료 시간만 서버 Clock을 사용한다")
    void acceptsOwnedActiveCommand(Command command) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
        given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID))
                .willReturn(Role.create(PLAYER_ID, RoleType.of("WORK"), "Developer", null));
        if (command != Command.CREATE) {
            RoleEvent event = RoleEvent.create(PLAYER_ID, ROLE_ID, "팀 회고", null, null, null);
            var participant = event.addParticipant(RoleEventParticipantType.PERSON, 3L);
            ReflectionTestUtils.setField(participant, "id", 9L);
            given(eventReader.getOwnedForUpdate(EVENT_ID, ROLE_ID, PLAYER_ID)).willReturn(event);
        }
        if (command == Command.COMPLETE) {
            given(clock.instant()).willReturn(Instant.parse("2026-10-02T00:00:00Z"));
        }
        if (command == Command.CREATE || command == Command.UPDATE || command == Command.COMPLETE
                || command == Command.CANCEL || command == Command.ADD_PARTICIPANT) {
            given(eventWriter.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));
        }

        execute(command);
    }

    private void assertRoleError(Command command, RoleError expected) {
        assertThatThrownBy(() -> execute(command)).isInstanceOfSatisfying(
                DomainException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }

    private void execute(Command command) {
        switch (command) {
            case CREATE -> service.create(ROLE_ID, new RoleEventCommand.Create("새 사건", null, null, null));
            case UPDATE -> service.update(ROLE_ID, EVENT_ID, new RoleEventCommand.Update("변경", null, null, null));
            case COMPLETE -> service.complete(ROLE_ID, EVENT_ID);
            case CANCEL -> service.cancel(ROLE_ID, EVENT_ID);
            case ADD_PARTICIPANT -> service.addParticipant(ROLE_ID, EVENT_ID,
                    new RoleEventCommand.AddParticipant("PERSON", 4L));
            case REMOVE_PARTICIPANT -> service.removeParticipant(ROLE_ID, EVENT_ID, 9L);
        }
    }
}
