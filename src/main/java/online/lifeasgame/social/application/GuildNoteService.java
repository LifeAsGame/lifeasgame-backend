package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.social.domain.PersonalGroupType;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuildNoteService {
    private final MemberAccess access;
    private final GuildNoteStore notes;
    private final PersonLookupApi persons;
    private final PlayerLookupApi players;
    private final CurrentPlayerAccessor currentPlayer;

    @Transactional(readOnly = true)
    public GuildNoteStore.Note find(Long guildId, Long target) {
        Long author = currentPlayer.currentPlayerIdOrThrow();
        access.requirePair(PersonalGroupType.GUILD, guildId, author, target, false);
        GuildNoteStore.Note note = notes.find(author, guildId, target);
        if (note == null) throw new DomainException(SocialError.GUILD_NOTE_NOT_FOUND);
        return note;
    }

    @Transactional
    public GuildNoteStore.Note put(Long guildId, Long target, Long personId, String text, Long version) {
        Long author = currentPlayer.currentPlayerIdOrThrow();
        access.requirePair(PersonalGroupType.GUILD, guildId, author, target, true);
        if (personId == null || personId <= 0) throw new DomainException(SocialError.GUILD_NOTE_INVALID_INPUT);
        var person = persons.getOwnedActive(personId, author);
        if (!players.findUserIdByPlayerId(target).equals(person.linkedUserId()))
            throw new DomainException(SocialError.GUILD_NOTE_CONFLICT);
        return notes.put(author, guildId, target, personId, normalized(text), version);
    }

    @Transactional
    public void delete(Long noteId) {
        if (noteId == null || noteId <= 0) throw new DomainException(SocialError.GUILD_NOTE_NOT_FOUND);
        notes.delete(currentPlayer.currentPlayerIdOrThrow(), noteId);
    }

    @Transactional(readOnly = true)
    public Page<GuildNoteStore.Note> page(Long personId, Long guildId, boolean includeHistory, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (guildId != null && guildId <= 0))
            throw new DomainException(SocialError.GUILD_NOTE_INVALID_INPUT);
        Long author = currentPlayer.currentPlayerIdOrThrow();
        persons.getOwned(personId, author);
        return notes.page(author, personId, guildId, includeHistory, PageRequest.of(page, size));
    }

    private static String normalized(String text) {
        if (text == null || text.isBlank()) return null;
        String value = text.strip();
        if (value.length() > 2000) throw new DomainException(SocialError.GUILD_NOTE_INVALID_INPUT);
        return value;
    }
}
