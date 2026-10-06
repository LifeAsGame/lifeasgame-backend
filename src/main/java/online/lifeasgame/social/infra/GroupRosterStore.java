package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GroupRosterStore {
    private final NamedParameterJdbcTemplate jdbc;

    public enum Type { GUILD, PARTY }
    public record GroupAccess(String status, Long leaderId, boolean member) {}
    public record Entry(Long id, Type type, Long groupId, String displayName, String groupRoleLabel,
                        Long linkedPlayerId, String memberStatus, long version, String status) {}
    public record Invitation(Long id, Long entryId, Type type, Long groupId, String groupName,
                             String rosterDisplayName, Long targetPlayerId, Long issuedByPlayerId,
                             String status, Instant expiresAt, boolean membershipWillBeCreated) {}

    private static String column(Type type) { return type == Type.GUILD ? "guild_id" : "party_id"; }
    private static String memberTable(Type type) { return type == Type.GUILD ? "guild_members" : "party_members"; }
    private static String groupTable(Type type) { return type == Type.GUILD ? "guilds" : "parties"; }
    private static LocalDateTime db(Instant instant) { return LocalDateTime.ofInstant(instant, ZoneOffset.UTC); }
    private static Instant instant(Timestamp timestamp) { return timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC); }
    private static MapSqlParameterSource p(Long groupId) { return new MapSqlParameterSource("groupId", groupId); }

    public GroupAccess access(Type type, Long groupId, Long actor) {
        String owner = column(type);
        return jdbc.query("SELECT g.status,g.leader_player_id,EXISTS(SELECT 1 FROM " + memberTable(type)
                        + " m WHERE m." + owner + "=g." + owner + " AND m.player_id=:actor) "
                        + "FROM " + groupTable(type) + " g WHERE g." + owner + "=:groupId",
                p(groupId).addValue("actor", actor), (rs, n) ->
                        new GroupAccess(rs.getString(1), rs.getLong(2), rs.getBoolean(3)))
                .stream().findFirst().orElse(null);
    }

    private static RowMapper<Entry> entryMapper(Type type) {
        return (rs, n) -> new Entry(rs.getLong("id"), type, rs.getLong("group_id"), rs.getString("display_name"),
                rs.getString("group_role_label"), rs.getObject("linked_player_id", Long.class),
                rs.getString("member_status"), rs.getLong("version"), rs.getString("status"));
    }

    private String entrySelect(Type type) {
        String owner = column(type);
        return "SELECT r.id,r." + owner + " group_id,r.display_name,r.group_role_label,r.linked_player_id,r.version,r.status,"
                + " CASE WHEN r.linked_player_id IS NULL THEN NULL WHEN m.player_id IS NULL THEN 'LEFT' ELSE 'ACTIVE' END member_status"
                + " FROM group_roster_entries r LEFT JOIN " + memberTable(type) + " m ON m." + owner
                + "=r." + owner + " AND m.player_id=r.linked_player_id";
    }

    public Entry entry(Type type, Long groupId, Long entryId, boolean lock) {
        var params = p(groupId).addValue("id", entryId);
        return jdbc.query(entrySelect(type) + " WHERE r." + column(type)
                        + "=:groupId AND r.id=:id" + (lock ? " FOR UPDATE" : ""), params, entryMapper(type))
                .stream().findFirst().orElse(null);
    }

    public List<Entry> entries(Type type, Long groupId, String status, String keyword, int page, int size) {
        var params = pageParams(groupId, status, keyword, page, size);
        return jdbc.query(entrySelect(type) + entryWhere(type, status, keyword) + " ORDER BY r.id DESC LIMIT :size OFFSET :offset",
                params, entryMapper(type));
    }

    public long entryCount(Type type, Long groupId, String status, String keyword) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM group_roster_entries r" + entryWhere(type, status, keyword),
                pageParams(groupId, status, keyword, 0, 1), Long.class);
    }

    public long unlinkedCount(Type type, Long groupId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM group_roster_entries WHERE " + column(type)
                + "=:groupId AND status='ACTIVE' AND linked_player_id IS NULL", p(groupId), Long.class);
    }

    private String entryWhere(Type type, String status, String keyword) {
        return " WHERE r." + column(type) + "=:groupId AND r.status='ACTIVE'"
                + ("UNLINKED".equals(status) ? " AND r.linked_player_id IS NULL" : "LINKED".equals(status)
                ? " AND r.linked_player_id IS NOT NULL" : "")
                + (keyword == null || keyword.isBlank() ? "" : " AND r.display_name LIKE :keyword ESCAPE '!'");
    }

    private MapSqlParameterSource pageParams(Long groupId, String status, String keyword, int page, int size) {
        String literal = keyword == null ? null : keyword.strip().replace("!", "!!")
                .replace("%", "!%").replace("_", "!_");
        return p(groupId).addValue("keyword", literal == null ? null : "%" + literal + "%")
                .addValue("size", size).addValue("offset", page * size);
    }

    public long create(Type type, Long groupId, String name, String label, Instant now) {
        var params = p(groupId).addValue("name", name).addValue("label", label).addValue("now", db(now));
        var keys = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO group_roster_entries (" + column(type)
                        + ",display_name,group_role_label,status,version,created_at,updated_at) "
                        + "VALUES (:groupId,:name,:label,'ACTIVE',0,:now,:now)", params, keys, new String[]{"id"});
        return keys.getKey().longValue();
    }

    public int update(Type type, Long groupId, Long id, long version, String name, String label, Instant now) {
        return jdbc.update("UPDATE group_roster_entries SET display_name=:name,group_role_label=:label,"
                        + "version=version+1,updated_at=:now WHERE " + column(type)
                        + "=:groupId AND id=:id AND status='ACTIVE' AND version=:version",
                p(groupId).addValue("id", id).addValue("version", version).addValue("name", name)
                        .addValue("label", label).addValue("now", db(now)));
    }

    public int delete(Type type, Long groupId, Long id, long version, Instant now) {
        return jdbc.update("UPDATE group_roster_entries SET status='DELETED',version=version+1,updated_at=:now "
                        + "WHERE " + column(type) + "=:groupId AND id=:id AND status='ACTIVE' AND version=:version",
                p(groupId).addValue("id", id).addValue("version", version).addValue("now", db(now)));
    }

    public int link(Type type, Long groupId, Long id, Long playerId, Instant now) {
        return jdbc.update("UPDATE group_roster_entries SET linked_player_id=:player,version=version+1,updated_at=:now "
                        + "WHERE " + column(type) + "=:groupId AND id=:id AND status='ACTIVE' AND linked_player_id IS NULL",
                p(groupId).addValue("id", id).addValue("player", playerId).addValue("now", db(now)));
    }

    public Invitation invitation(Long id, boolean lock) {
        var params = new MapSqlParameterSource("id", id);
        return jdbc.query(invitationSelect() + " WHERE i.id=:id" + (lock ? " FOR UPDATE" : ""), params,
                invitationMapper()).stream().findFirst().orElse(null);
    }

    public Invitation pendingForEntry(Long entryId) {
        return jdbc.query(invitationSelect() + " WHERE i.roster_entry_id=:entry AND i.status='PENDING'",
                new MapSqlParameterSource("entry", entryId), invitationMapper()).stream().findFirst().orElse(null);
    }

    public long invite(Long entryId, Long target, Long issuer, Instant expiresAt, Instant now) {
        var params = new MapSqlParameterSource("entry", entryId).addValue("target", target)
                .addValue("issuer", issuer).addValue("expires", db(expiresAt)).addValue("now", db(now));
        var keys = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO group_roster_invitations (roster_entry_id,target_player_id,issued_by_player_id,"
                        + "status,expires_at,created_at,updated_at) VALUES (:entry,:target,:issuer,'PENDING',:expires,:now,:now)",
                params, keys, new String[]{"id"});
        return keys.getKey().longValue();
    }

    public void transition(Long id, String from, String to, Instant now) {
        jdbc.update("UPDATE group_roster_invitations SET status=:to,updated_at=:now WHERE id=:id AND status=:from",
                new MapSqlParameterSource("id", id).addValue("from", from).addValue("to", to).addValue("now", db(now)));
    }

    public void cancelPending(Type type, Long groupId, Instant now) {
        jdbc.update("UPDATE group_roster_invitations SET status='CANCELED',updated_at=:now "
                        + "WHERE status='PENDING' AND EXISTS (SELECT 1 FROM group_roster_entries r "
                        + "WHERE r.id=group_roster_invitations.roster_entry_id AND r." + column(type)
                        + "=:groupId)", p(groupId).addValue("now", db(now)));
    }

    public List<Invitation> pending(Type type, Long groupId, Instant now, int page, int size) {
        return jdbc.query(invitationSelect() + " WHERE r." + column(type) + "=:groupId AND r.status='ACTIVE' "
                        + "AND i.status='PENDING' AND i.expires_at>:now AND i.issued_by_player_id="
                        + "COALESCE(g1.leader_player_id,g2.leader_player_id) "
                        + "ORDER BY i.id DESC LIMIT :size OFFSET :offset",
                p(groupId).addValue("now", db(now)).addValue("size", size).addValue("offset", page * size), invitationMapper());
    }

    public long pendingCount(Type type, Long groupId, Instant now) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM group_roster_invitations i JOIN group_roster_entries r "
                        + "ON r.id=i.roster_entry_id JOIN " + groupTable(type) + " g ON g."
                        + column(type) + "=r." + column(type) + " WHERE r." + column(type)
                        + "=:groupId AND r.status='ACTIVE' AND i.status='PENDING' AND i.expires_at>:now "
                        + "AND i.issued_by_player_id=g.leader_player_id",
                p(groupId).addValue("now", db(now)), Long.class);
    }

    public List<Invitation> mine(Long actor, Instant now, int page, int size) {
        return jdbc.query(invitationSelect() + " WHERE i.target_player_id=:actor AND i.status='PENDING' "
                        + "AND i.expires_at>:now AND r.status='ACTIVE' "
                        + "AND COALESCE(g1.status,g2.status)='ACTIVE' "
                        + "AND i.issued_by_player_id=COALESCE(g1.leader_player_id,g2.leader_player_id) "
                        + "ORDER BY i.id DESC LIMIT :size OFFSET :offset",
                new MapSqlParameterSource("actor", actor).addValue("now", db(now))
                        .addValue("size", size).addValue("offset", page * size), invitationMapper());
    }

    public long mineCount(Long actor, Instant now) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM group_roster_invitations i JOIN group_roster_entries r "
                        + "ON r.id=i.roster_entry_id LEFT JOIN guilds g1 ON g1.guild_id=r.guild_id "
                        + "LEFT JOIN parties g2 ON g2.party_id=r.party_id "
                        + "WHERE i.target_player_id=:actor AND i.status='PENDING' AND i.expires_at>:now "
                        + "AND r.status='ACTIVE' AND ((g1.status='ACTIVE' AND g1.leader_player_id=i.issued_by_player_id) "
                        + "OR (g2.status='ACTIVE' AND g2.leader_player_id=i.issued_by_player_id))",
                new MapSqlParameterSource("actor", actor).addValue("now", db(now)), Long.class);
    }

    private String invitationSelect() {
        return "SELECT i.id,i.roster_entry_id,r.guild_id,r.party_id,COALESCE(g1.name_original,g2.name_original) group_name,"
                + "r.display_name,i.target_player_id,i.issued_by_player_id,i.status,i.expires_at,"
                + "CASE WHEN gm.player_id IS NULL AND pm.player_id IS NULL THEN 1 ELSE 0 END needs_membership,"
                + "COALESCE(g1.leader_player_id,g2.leader_player_id) current_leader,"
                + "COALESCE(g1.status,g2.status) group_status "
                + "FROM group_roster_invitations i JOIN group_roster_entries r ON r.id=i.roster_entry_id "
                + "LEFT JOIN guilds g1 ON g1.guild_id=r.guild_id LEFT JOIN parties g2 ON g2.party_id=r.party_id "
                + "LEFT JOIN guild_members gm ON gm.guild_id=r.guild_id AND gm.player_id=i.target_player_id "
                + "LEFT JOIN party_members pm ON pm.party_id=r.party_id AND pm.player_id=i.target_player_id ";
    }

    private RowMapper<Invitation> invitationMapper() {
        return (rs, n) -> {
            Long guildId = rs.getObject("guild_id", Long.class);
            Type type = guildId == null ? Type.PARTY : Type.GUILD;
            return new Invitation(rs.getLong("id"), rs.getLong("roster_entry_id"), type,
                    guildId == null ? rs.getLong("party_id") : guildId,
                    rs.getString("group_name"), rs.getString("display_name"), rs.getLong("target_player_id"),
                    rs.getLong("issued_by_player_id"), rs.getString("status"), instant(rs.getTimestamp("expires_at")),
                    rs.getInt("needs_membership") == 1);
        };
    }
}
