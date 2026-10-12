package online.lifeasgame.person.application.internal;

import online.lifeasgame.person.application.command.PersonCommand;

public interface PersonLinkApi {
    Link find(Long ownerPlayerId, Long userId);
    Link select(Long ownerPlayerId, Long userId, Long personId);
    Link create(Long ownerPlayerId, Long userId, PersonCommand.Create command);
    void unlink(Long ownerPlayerId, Long personId);

    record Link(Long personId, String personStatus) {
        public static Link absent() { return new Link(null, null); }
    }
}
