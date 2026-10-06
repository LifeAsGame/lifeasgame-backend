package online.lifeasgame.social.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.domain.error.SocialError;

public record GuildNoteText(String value) {
    public GuildNoteText {
        value = value == null || value.isBlank() ? null : value.strip();
        if (value != null && value.length() > 2000)
            throw new DomainException(SocialError.GUILD_NOTE_INVALID_INPUT);
    }
}
