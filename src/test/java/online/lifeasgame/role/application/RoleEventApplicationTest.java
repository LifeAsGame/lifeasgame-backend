package online.lifeasgame.role.application;

import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.role.application.command.RoleEventCommand;
import online.lifeasgame.role.application.result.RoleEventResult;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleEvent command가 GATED이면")
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
    @DisplayName("인증된 소유자의 직접 호출도 aggregate 변경과 외부 조회 전에 거부한다")
    void rejectsOwnedCommand(Command command) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
        Role role = Role.create(PLAYER_ID, RoleType.of("WORK"), "Developer", null);
        RoleEvent event = RoleEvent.create(PLAYER_ID, ROLE_ID, "팀 회고", null, null, null);
        var participant = event.addParticipant(RoleEventParticipantType.PERSON, 3L);
        ReflectionTestUtils.setField(participant, "id", 9L);
        var before = RoleEventResult.Detail.from(event);
        if (command == Command.CREATE) {
            given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID)).willReturn(role);
        } else {
            given(eventReader.getOwnedForUpdate(EVENT_ID, ROLE_ID, PLAYER_ID)).willReturn(event);
        }

        assertRoleError(command, RoleError.ROLE_EVENT_COMMAND_GATED);

        assertThat(RoleEventResult.Detail.from(event)).isEqualTo(before);
        verifyNoInteractions(eventWriter, personLookupApi, userLookupApi, clock);
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    @DisplayName("타인 소유 대상의 not found 보호를 gate보다 먼저 적용한다")
    void preservesOwnership(Command command) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
        RoleError error = command == Command.CREATE
                ? RoleError.ROLE_NOT_FOUND : RoleError.ROLE_EVENT_NOT_FOUND;
        if (command == Command.CREATE) {
            given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID))
                    .willThrow(new DomainException(error));
        } else {
            given(eventReader.getOwnedForUpdate(EVENT_ID, ROLE_ID, PLAYER_ID))
                    .willThrow(new DomainException(error));
        }

        assertRoleError(command, error);

        verifyNoInteractions(eventWriter, personLookupApi, userLookupApi, clock);
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    @DisplayName("인증 없는 직접 호출은 저장소 조회 전에 거부한다")
    void preservesAuthentication(Command command) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willThrow(new AuthException(AuthError.UNAUTHORIZED));

        assertThatThrownBy(() -> execute(command)).isInstanceOfSatisfying(
                AuthException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(AuthError.UNAUTHORIZED));

        verifyNoInteractions(roleReader, eventReader, eventWriter, personLookupApi, userLookupApi, clock);
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
