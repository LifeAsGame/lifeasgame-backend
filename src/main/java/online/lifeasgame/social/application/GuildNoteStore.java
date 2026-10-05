package online.lifeasgame.social.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GuildNoteStore {
    Note find(Long author, Long guildId, Long target);
    Note put(Long author, Long guildId, Long target, Long personId, String text, Long version);
    void delete(Long author, Long id);
    Page<Note> page(Long author, Long personId, Long guildId, boolean includeHistory, Pageable pageable);
    record Note(Long id, Long personId, Long guildId, Long targetMemberPlayerId,
                String text, Long version, String availability, String guildName) {}
}
