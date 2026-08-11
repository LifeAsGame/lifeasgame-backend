package online.lifeasgame.role.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.error.RoleError;

public enum RoleEventParticipantType {
    PERSON,
    SERVICE_USER;

    public static RoleEventParticipantType parse(String raw) {
        if (raw == null) {
            throw invalid();
        }
        try {
            return valueOf(raw);
        } catch (IllegalArgumentException exception) {
            throw new DomainException(
                    RoleError.INVALID_ROLE_EVENT_PARTICIPANT_TYPE,
                    null,
                    exception
            );
        }
    }

    private static DomainException invalid() {
        return new DomainException(
                RoleError.INVALID_ROLE_EVENT_PARTICIPANT_TYPE
        );
    }
}
