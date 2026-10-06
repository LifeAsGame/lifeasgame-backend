package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.GroupActivityResult;
import online.lifeasgame.social.domain.ActivityGroupType;
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
public class GroupActivityStore {
    private final NamedParameterJdbcTemplate jdbc;

    public record Group(Long leaderId, String status) {}
    public record Member(Long id, LocalDateTime joinedAt) {}
    public record Row(Long id, String title, String description, String location, Instant startsAt,
                      Instant endsAt, String status, Long creatorId, Instant createdAt, Instant updatedAt,
                      long version, long participantCount, boolean myRsvp) {}
    public record Participant(Long playerId, Instant joinedAt) {}

    private static String column(ActivityGroupType type) { return type == ActivityGroupType.PARTY ? "party_id" : "role_party_id"; }
    private static String groupTable(ActivityGroupType type) { return type == ActivityGroupType.PARTY ? "parties" : "role_parties"; }
    private static String groupPk(ActivityGroupType type) { return type == ActivityGroupType.PARTY ? "party_id" : "id"; }
    private static String memberTable(ActivityGroupType type) { return type == ActivityGroupType.PARTY ? "party_members" : "role_party_members"; }
    private static String memberPk(ActivityGroupType type) { return type == ActivityGroupType.PARTY ? "party_member_id" : "id"; }
    private static String memberGroupFk(ActivityGroupType type) { return type == ActivityGroupType.PARTY ? "party_id" : "role_party_id"; }
    private static String active(ActivityGroupType type) { return type == ActivityGroupType.PARTY ? "" : " AND m.left_at IS NULL"; }
    private static LocalDateTime db(Instant instant) { return LocalDateTime.ofInstant(instant, ZoneOffset.UTC); }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toLocalDateTime().toInstant(ZoneOffset.UTC); }
    private static MapSqlParameterSource params(Long groupId, Long actor) {
        return new MapSqlParameterSource("groupId", groupId).addValue("actor", actor);
    }

    public Group group(ActivityGroupType type, Long groupId, boolean lock) {
        String sql = "SELECT leader_player_id, status FROM " + groupTable(type) + " WHERE " + groupPk(type)
                + " = :groupId" + (lock ? " FOR UPDATE" : "");
        return jdbc.query(sql, params(groupId, null), (rs, n) -> new Group(rs.getLong(1), rs.getString(2)))
                .stream().findFirst().orElse(null);
    }

    public Member member(ActivityGroupType type, Long groupId, Long playerId) {
        String sql = "SELECT m." + memberPk(type) + ", m.joined_at FROM " + memberTable(type)
                + " m WHERE m." + memberGroupFk(type) + " = :groupId AND m.player_id = :actor" + active(type);
        return jdbc.query(sql, params(groupId, playerId), (rs, n) ->
                new Member(rs.getLong(1), rs.getTimestamp(2).toLocalDateTime())).stream().findFirst().orElse(null);
    }

    public boolean editor(ActivityGroupType type, Long groupId, Long actor, Member member) {
        String sql = "SELECT COUNT(*) FROM group_activity_editors WHERE " + column(type)
                + " = :groupId AND player_id = :actor AND member_id = :memberId AND member_joined_at = :joinedAt";
        return jdbc.queryForObject(sql, params(groupId, actor).addValue("memberId", member.id())
                .addValue("joinedAt", member.joinedAt()), Long.class) > 0;
    }

    public void grant(ActivityGroupType type, Long groupId, Long playerId, Member member, Instant now) {
        String sql = "INSERT INTO group_activity_editors (" + column(type)
                + ", player_id, member_id, member_joined_at, created_at) VALUES (:groupId,:actor,:memberId,:joinedAt,:now)"
                + " ON DUPLICATE KEY UPDATE member_id=VALUES(member_id), member_joined_at=VALUES(member_joined_at), created_at=VALUES(created_at)";
        jdbc.update(sql, params(groupId, playerId).addValue("memberId", member.id())
                .addValue("joinedAt", member.joinedAt()).addValue("now", db(now)));
    }

    public void revoke(ActivityGroupType type, Long groupId, Long playerId) {
        jdbc.update("DELETE FROM group_activity_editors WHERE " + column(type) + " = :groupId AND player_id = :actor",
                params(groupId, playerId));
    }

    public List<GroupActivityResult.Editor> editors(ActivityGroupType type, Long groupId, int page, int size) {
        String sql = "SELECT e.player_id FROM group_activity_editors e JOIN " + memberTable(type)
                + " m ON m." + memberGroupFk(type) + " = e." + column(type)
                + " AND m.player_id = e.player_id AND m." + memberPk(type)
                + " = e.member_id AND m.joined_at = e.member_joined_at" + active(type)
                + " WHERE e." + column(type) + " = :groupId ORDER BY e.player_id LIMIT :size OFFSET :offset";
        return jdbc.query(sql, params(groupId, null).addValue("size", size).addValue("offset", page * size),
                (rs, n) -> new GroupActivityResult.Editor(rs.getLong(1)));
    }

    public long editorCount(ActivityGroupType type, Long groupId) {
        String sql = "SELECT COUNT(*) FROM group_activity_editors e JOIN " + memberTable(type)
                + " m ON m." + memberGroupFk(type) + " = e." + column(type)
                + " AND m.player_id = e.player_id AND m." + memberPk(type)
                + " = e.member_id AND m.joined_at = e.member_joined_at" + active(type)
                + " WHERE e." + column(type) + " = :groupId";
        return jdbc.queryForObject(sql, params(groupId, null), Long.class);
    }

    public Row byKey(ActivityGroupType type, Long groupId, Long actor, String key) {
        String sql = select(type) + " WHERE a." + column(type) + " = :groupId AND a.created_by_player_id = :actor AND a.client_request_id = :key";
        return jdbc.query(sql, params(groupId, actor).addValue("key", key), MAPPER).stream().findFirst().orElse(null);
    }

    public String requestHash(Long activityId) {
        return jdbc.queryForObject("SELECT request_hash FROM group_activities WHERE id = :id",
                new MapSqlParameterSource("id", activityId), String.class);
    }

    public long create(ActivityGroupType type, Long groupId, Long actor, String key, String hash,
                       String title, String description, String location, Instant starts, Instant ends, Instant now) {
        var p = params(groupId, actor).addValue("key", key).addValue("hash", hash)
                .addValue("title", title).addValue("description", description).addValue("location", location)
                .addValue("starts", db(starts)).addValue("ends", db(ends)).addValue("now", db(now));
        var keys = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO group_activities (" + column(type)
                + ", created_by_player_id, client_request_id, request_hash, title, shared_description, location, starts_at, ends_at, status, version, created_at, updated_at)"
                + " VALUES (:groupId,:actor,:key,:hash,:title,:description,:location,:starts,:ends,'PLANNED',0,:now,:now)", p, keys);
        return keys.getKey().longValue();
    }

    public Row activity(ActivityGroupType type, Long groupId, Long activityId, Long actor) {
        return jdbc.query(select(type) + " WHERE a." + column(type) + " = :groupId AND a.id = :id",
                params(groupId, actor).addValue("id", activityId), MAPPER).stream().findFirst().orElse(null);
    }

    public List<Row> activities(ActivityGroupType type, Long groupId, Long actor, int page, int size) {
        return jdbc.query(select(type) + " WHERE a." + column(type)
                        + " = :groupId ORDER BY a.starts_at, a.id LIMIT :size OFFSET :offset",
                params(groupId, actor).addValue("size", size).addValue("offset", page * size), MAPPER);
    }

    public long count(ActivityGroupType type, Long groupId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM group_activities WHERE " + column(type) + " = :groupId",
                params(groupId, null), Long.class);
    }

    public void update(long id, long version, String title, String description, String location,
                       Instant starts, Instant ends, Instant now) {
        jdbc.update("UPDATE group_activities SET title=:title, shared_description=:description, location=:location,"
                + " starts_at=:starts, ends_at=:ends, version=version+1, updated_at=:now WHERE id=:id AND version=:version AND status='PLANNED'",
                new MapSqlParameterSource("id", id).addValue("version", version).addValue("title", title)
                        .addValue("description", description).addValue("location", location)
                        .addValue("starts", db(starts)).addValue("ends", db(ends)).addValue("now", db(now)));
    }

    public void finish(long id, long version, String status, Instant now) {
        jdbc.update("UPDATE group_activities SET status=:status, version=version+1, updated_at=:now WHERE id=:id AND version=:version AND status='PLANNED'",
                new MapSqlParameterSource("id", id).addValue("version", version).addValue("status", status).addValue("now", db(now)));
    }

    public void rsvp(Long activityId, Long actor, Member member, boolean active, Instant now) {
        if (active) {
            jdbc.update("INSERT INTO group_activity_rsvps (activity_id, player_id, member_id, member_joined_at, joined_at, active, created_at, updated_at)"
                    + " VALUES (:id,:actor,:memberId,:joinedAt,:now,TRUE,:now,:now)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " joined_at=IF(active AND member_id=VALUES(member_id) AND member_joined_at=VALUES(member_joined_at), joined_at, VALUES(joined_at)),"
                    + " member_id=VALUES(member_id), member_joined_at=VALUES(member_joined_at),"
                    + " active=TRUE, updated_at=VALUES(updated_at)",
                    new MapSqlParameterSource("id", activityId).addValue("actor", actor)
                            .addValue("memberId", member.id()).addValue("joinedAt", member.joinedAt()).addValue("now", db(now)));
        } else {
            jdbc.update("UPDATE group_activity_rsvps SET active=FALSE, updated_at=:now WHERE activity_id=:id AND player_id=:actor"
                            + " AND member_id=:memberId AND member_joined_at=:joinedAt AND active=TRUE",
                    new MapSqlParameterSource("id", activityId).addValue("actor", actor)
                            .addValue("memberId", member.id()).addValue("joinedAt", member.joinedAt()).addValue("now", db(now)));
        }
    }

    public List<Participant> participants(ActivityGroupType type, Long activityId, Long groupId, int page, int size) {
        String sql = "SELECT r.player_id, r.joined_at FROM group_activity_rsvps r JOIN " + memberTable(type)
                + " m ON m." + memberGroupFk(type) + " = :groupId AND m.player_id = r.player_id"
                + " AND m." + memberPk(type) + " = r.member_id AND m.joined_at = r.member_joined_at" + active(type)
                + " WHERE r.activity_id = :id AND r.active = TRUE ORDER BY r.joined_at, r.id LIMIT :size OFFSET :offset";
        return jdbc.query(sql, new MapSqlParameterSource("id", activityId).addValue("groupId", groupId)
                .addValue("size", size).addValue("offset", page * size),
                (rs, n) -> new Participant(rs.getLong(1), instant(rs.getTimestamp(2))));
    }

    private static String select(ActivityGroupType type) {
        String count = "SELECT COUNT(*) FROM group_activity_rsvps r JOIN " + memberTable(type)
                + " m ON m." + memberGroupFk(type) + " = a." + column(type)
                + " AND m.player_id = r.player_id AND m." + memberPk(type)
                + " = r.member_id AND m.joined_at = r.member_joined_at" + active(type)
                + " WHERE r.activity_id=a.id AND r.active=TRUE";
        return "SELECT a.id,a.title,a.shared_description,a.location,a.starts_at,a.ends_at,a.status,"
                + "a.created_by_player_id,a.created_at,a.updated_at,a.version,(" + count + ") AS participant_count,"
                + "EXISTS (SELECT 1 FROM group_activity_rsvps r JOIN " + memberTable(type)
                + " m ON m." + memberGroupFk(type) + " = a." + column(type)
                + " AND m.player_id = r.player_id AND m." + memberPk(type)
                + " = r.member_id AND m.joined_at = r.member_joined_at" + active(type)
                + " WHERE r.activity_id=a.id AND r.player_id=:actor AND r.active=TRUE) AS my_rsvp"
                + " FROM group_activities a";
    }

    private static final RowMapper<Row> MAPPER = (rs, n) -> new Row(rs.getLong("id"), rs.getString("title"),
            rs.getString("shared_description"), rs.getString("location"), instant(rs.getTimestamp("starts_at")),
            instant(rs.getTimestamp("ends_at")), rs.getString("status"), rs.getLong("created_by_player_id"),
            instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("updated_at")), rs.getLong("version"),
            rs.getLong("participant_count"), rs.getBoolean("my_rsvp"));
}
