package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.GuildNoteStore;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GuildNoteSqlStore implements GuildNoteStore {
    private final NamedParameterJdbcTemplate jdbc;

    private static final String FROM = """
        FROM private_guild_member_notes n
        LEFT JOIN guilds g ON g.guild_id = n.guild_id AND g.status = 'ACTIVE'
          AND EXISTS (SELECT 1 FROM guild_members am WHERE am.guild_id = g.guild_id AND am.player_id = :author)
        LEFT JOIN guild_members tm ON tm.guild_id = g.guild_id AND tm.player_id = n.target_member_player_id
        WHERE n.author_player_id = :author
        """;
    private static final String SELECT = """
        SELECT n.id, n.person_id, n.guild_id, n.target_member_player_id, n.note_text, n.version,
          CASE WHEN g.guild_id IS NOT NULL AND tm.player_id IS NOT NULL THEN 'CURRENT' ELSE 'HISTORY' END AS availability,
          g.name_original AS guild_name
        """;

    @Override
    public Note find(Long author, Long guildId, Long target) {
        var p = params(author).addValue("guildId", guildId).addValue("target", target);
        List<Note> rows = jdbc.query(SELECT + FROM + " AND n.guild_id = :guildId AND n.target_member_player_id = :target",
                p, GuildNoteSqlStore::map);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Override
    public Note put(Long author, Long guildId, Long target, Long personId, String text, Long version) {
        var p = params(author).addValue("guildId", guildId).addValue("target", target)
                .addValue("personId", personId).addValue("text", text).addValue("version", version);
        Note existing = find(author, guildId, target);
        if (existing == null) {
            if (version != null) throw conflict();
            jdbc.update("""
                    INSERT INTO private_guild_member_notes
                      (author_player_id, guild_id, target_member_player_id, person_id, note_text, version, created_at, updated_at)
                    VALUES (:author, :guildId, :target, :personId, :text, 0, NOW(6), NOW(6))
                    """, p);
        } else {
            if (!existing.personId().equals(personId)) throw conflict();
            if (version == null) {
                if (!java.util.Objects.equals(existing.text(), text)) throw conflict();
                return existing;
            }
            int changed = jdbc.update("""
                    UPDATE private_guild_member_notes SET note_text = :text, version = version + 1,
                      updated_at = NOW(6)
                    WHERE author_player_id = :author AND guild_id = :guildId
                      AND target_member_player_id = :target AND version = :version
                    """, p);
            if (changed != 1) throw conflict();
        }
        return find(author, guildId, target);
    }

    @Override
    public void delete(Long author, Long id) {
        int changed = jdbc.update("DELETE FROM private_guild_member_notes WHERE author_player_id = :author AND id = :id",
                params(author).addValue("id", id));
        if (changed != 1) throw new DomainException(SocialError.GUILD_NOTE_NOT_FOUND);
    }

    @Override
    public Page<Note> page(Long author, Long personId, Long guildId, String keyword, boolean includeHistory, Pageable pageable) {
        var p = params(author).addValue("personId", personId).addValue("guildId", guildId)
                .addValue("keyword", keyword).addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        String where = FROM + " AND n.person_id = :personId AND (:guildId IS NULL OR n.guild_id = :guildId)"
                + " AND LOCATE(LOWER(:keyword), LOWER(COALESCE(n.note_text, ''))) > 0"
                + (includeHistory ? "" : " AND g.guild_id IS NOT NULL AND tm.player_id IS NOT NULL");
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + where, p, Long.class);
        List<Note> notes = jdbc.query(SELECT + where + " ORDER BY n.id DESC LIMIT :limit OFFSET :offset",
                p, GuildNoteSqlStore::map);
        return new PageImpl<>(notes, pageable, total == null ? 0 : total);
    }

    private static MapSqlParameterSource params(Long author) { return new MapSqlParameterSource("author", author); }
    private static Note map(ResultSet rs, int row) throws SQLException {
        return new Note(rs.getLong("id"), rs.getLong("person_id"), rs.getLong("guild_id"),
                rs.getLong("target_member_player_id"), rs.getString("note_text"), rs.getLong("version"),
                rs.getString("availability"), rs.getString("guild_name"));
    }
    private static DomainException conflict() { return new DomainException(SocialError.GUILD_NOTE_CONFLICT); }
}
