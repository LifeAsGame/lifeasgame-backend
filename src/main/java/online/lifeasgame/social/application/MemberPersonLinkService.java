package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.application.internal.PersonLinkApi;
import online.lifeasgame.social.domain.PersonalGroupType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberPersonLinkService {
    private final MemberAccess access;
    private final PlayerLookupApi players;
    private final PersonLinkApi persons;
    private final CurrentPlayerAccessor currentPlayer;

    @Transactional(readOnly = true)
    public Identity find(PersonalGroupType type, Long groupId, Long memberPlayerId) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        access.requirePair(type, groupId, owner, memberPlayerId, false);
        Long userId = players.findUserIdByPlayerId(memberPlayerId);
        return identity(type, groupId, memberPlayerId, persons.find(owner, userId));
    }

    @Transactional
    public Identity select(PersonalGroupType type, Long groupId, Long memberPlayerId, Long personId) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        access.requirePair(type, groupId, owner, memberPlayerId, true);
        Long userId = players.findUserIdByPlayerId(memberPlayerId);
        return identity(type, groupId, memberPlayerId, persons.select(owner, userId, personId));
    }

    @Transactional
    public Identity create(PersonalGroupType type, Long groupId, Long memberPlayerId, PersonCommand.Create command) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        access.requirePair(type, groupId, owner, memberPlayerId, true);
        Long userId = players.findUserIdByPlayerId(memberPlayerId);
        return identity(type, groupId, memberPlayerId, persons.create(owner, userId, command));
    }

    private static Identity identity(PersonalGroupType type, Long groupId, Long memberPlayerId, PersonLinkApi.Link link) {
        return new Identity(type, groupId, memberPlayerId, link.personId(), link.personStatus());
    }

    public record Identity(PersonalGroupType groupType, Long groupId, Long memberPlayerId,
                           Long personId, String personStatus) {}
}
