package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.role.application.command.RoleEventCommand;
import online.lifeasgame.role.application.result.RoleEventResult;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleEvent;
import online.lifeasgame.role.domain.RoleEventParticipant;
import online.lifeasgame.role.domain.RoleEventParticipantType;
import online.lifeasgame.role.domain.RoleStatus;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.user.application.internal.UserLookupApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RoleEventService {

    private final RoleReader roleReader;
    private final RoleEventReader eventReader;
    private final RoleEventWriter eventWriter;
    private final PersonLookupApi personLookupApi;
    private final UserLookupApi userLookupApi;
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final Clock clock;

    @Transactional
    public RoleEventResult.Detail create(
            Long roleId,
            RoleEventCommand.Create command
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Role role = roleReader.getOwnedForUpdate(roleId, playerId);
        requireActive(role);
        return RoleEventResult.Detail.from(eventWriter.saveAndFlush(
                RoleEvent.create(
                        playerId,
                        roleId,
                        command.title(),
                        command.description(),
                        command.startsAt(),
                        command.endsAt()
                )
        ));
    }

    @Transactional
    public RoleEventResult.Detail update(
            Long roleId,
            Long eventId,
            RoleEventCommand.Update command
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        RoleEvent event = eventReader.getOwnedForUpdate(
                eventId,
                roleId,
                playerId
        );
        event.update(
                command.title(),
                command.description(),
                command.startsAt(),
                command.endsAt()
        );
        return RoleEventResult.Detail.from(eventWriter.saveAndFlush(event));
    }

    @Transactional
    public RoleEventResult.Detail complete(Long roleId, Long eventId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        RoleEvent event = eventReader.getOwnedForUpdate(
                eventId,
                roleId,
                playerId
        );
        event.complete(clock.instant());
        return RoleEventResult.Detail.from(eventWriter.saveAndFlush(event));
    }

    @Transactional
    public RoleEventResult.Detail cancel(Long roleId, Long eventId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        RoleEvent event = eventReader.getOwnedForUpdate(
                eventId,
                roleId,
                playerId
        );
        event.cancel();
        return RoleEventResult.Detail.from(eventWriter.saveAndFlush(event));
    }

    @Transactional
    public RoleEventResult.Participant addParticipant(
            Long roleId,
            Long eventId,
            RoleEventCommand.AddParticipant command
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        RoleEvent event = eventReader.getOwnedForUpdate(
                eventId,
                roleId,
                playerId
        );
        RoleEventParticipantType type = RoleEventParticipantType.parse(
                command.participantType()
        );
        validateParticipant(type, command.participantId(), playerId);
        RoleEventParticipant participant = event.addParticipant(
                type,
                command.participantId()
        );
        eventWriter.saveAndFlush(event);
        return RoleEventResult.Participant.from(participant);
    }

    @Transactional
    public void removeParticipant(
            Long roleId,
            Long eventId,
            Long participantLinkId
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        RoleEvent event = eventReader.getOwnedForUpdate(
                eventId,
                roleId,
                playerId
        );
        event.removeParticipant(participantLinkId);
        eventWriter.saveAndFlush(event);
    }

    private void validateParticipant(
            RoleEventParticipantType type,
            Long participantId,
            Long playerId
    ) {
        switch (type) {
            case PERSON -> personLookupApi.getOwnedActive(
                    participantId,
                    playerId
            );
            case SERVICE_USER -> userLookupApi.getActive(participantId);
        }
    }

    private void requireActive(Role role) {
        if (role.getStatus() == RoleStatus.ARCHIVED) {
            throw new DomainException(RoleError.ROLE_ARCHIVED);
        }
    }
}
