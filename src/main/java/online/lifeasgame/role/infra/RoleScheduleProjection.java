package online.lifeasgame.role.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.application.query.RoleScheduleQuery;
import online.lifeasgame.role.application.result.RoleScheduleResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Read model only: joins source tables with current access in SQL before count and pagination. */
@Repository
@RequiredArgsConstructor
public class RoleScheduleProjection implements RoleScheduleQuery {
    // Hibernate persists Instant-valued RoleEvent/GuildEvent DATETIME(6) columns in UTC.
    private static final ZoneId DATABASE_ZONE = ZoneId.of("UTC");
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public RoleScheduleResult.Page find(Long owner, Long roleId, Criteria criteria) {
        var params = new MapSqlParameterSource("owner", owner)
                .addValue("roleId", roleId)
                .addValue("status", criteria.status().name())
                .addValue("from", databaseTime(criteria.from()))
                .addValue("to", databaseTime(criteria.to()))
                .addValue("size", criteria.size())
                .addValue("offset", criteria.page() * criteria.size());
        var sources = new ArrayList<String>();
        if (criteria.source() == Source.ALL || criteria.source() == Source.PERSONAL)
            sources.add(personal(criteria.time()));
        if (criteria.time() == TimeMode.DATED) {
            if (criteria.source() == Source.ALL || criteria.source() == Source.GUILD || criteria.source() == Source.SHARED)
                sources.add(guild(criteria.participating()));
            if (criteria.source() == Source.ALL || criteria.source() == Source.PARTY || criteria.source() == Source.SHARED)
                sources.add(activity("PARTY", criteria.participating()));
            if (criteria.source() == Source.ALL || criteria.source() == Source.ROLE_PARTY || criteria.source() == Source.SHARED)
                sources.add(activity("ROLE_PARTY", criteria.participating()));
        }
        String order = criteria.time() == TimeMode.DATED
                ? "sort_at ASC, source_type ASC, source_id ASC"
                : "source_type ASC, source_id ASC";
        String sql = "WITH schedule AS (" + String.join(" UNION ALL ", sources) + "), "
                + "totals AS (SELECT COUNT(*) AS total_elements FROM schedule), "
                + "page AS (SELECT * FROM schedule ORDER BY " + order + " LIMIT :size OFFSET :offset) "
                + "SELECT totals.total_elements, page.* FROM totals LEFT JOIN page ON TRUE "
                + "ORDER BY " + order;
        // A single DB statement gives rows, RSVP and total the same live-access snapshot.
        return jdbc.query(sql, params, rs -> {
            long total = 0;
            List<RoleScheduleResult.Row> rows = new ArrayList<>();
            while (rs.next()) {
                total = rs.getLong("total_elements");
                if (rs.getObject("source_id") != null) rows.add(row(rs));
            }
            return RoleScheduleResult.Page.of(rows, criteria.page(), criteria.size(), total);
        });
    }

    private static String personal(TimeMode time) {
        String window = time == TimeMode.UNSCHEDULED
                ? "AND e.starts_at IS NULL AND e.ends_at IS NULL "
                : """
                  AND ((e.starts_at IS NOT NULL AND e.ends_at IS NOT NULL
                        AND e.ends_at > e.starts_at AND e.starts_at < :to AND e.ends_at > :from)
                    OR ((e.starts_at IS NULL OR e.ends_at IS NULL OR e.starts_at = e.ends_at)
                        AND COALESCE(e.starts_at, e.ends_at) >= :from
                        AND COALESCE(e.starts_at, e.ends_at) < :to))
                  """;
        return """
                SELECT 'ROLE_EVENT' AS source_type, e.id AS source_id, e.role_id AS role_id,
                       NULL AS guild_id, NULL AS guild_name,
                       NULL AS group_type, NULL AS group_id, NULL AS group_name, e.title AS title,
                       e.starts_at AS starts_at, e.ends_at AS ends_at, e.status AS status,
                       NULL AS my_rsvp, COALESCE(e.starts_at, e.ends_at) AS sort_at
                  FROM role_events e
                 WHERE e.player_id = :owner AND e.role_id = :roleId
                   AND (:status = 'ALL' OR e.status = :status)
                """ + window;
    }

    private static String guild(boolean participating) {
        return """
                SELECT 'GUILD_EVENT' AS source_type, e.id AS source_id, NULL AS role_id,
                       g.guild_id AS guild_id, g.name_original AS guild_name,
                       NULL AS group_type, NULL AS group_id, NULL AS group_name, e.title AS title,
                       e.starts_at AS starts_at, e.ends_at AS ends_at, e.status AS status,
                       EXISTS (SELECT 1 FROM guild_event_rsvps r
                                WHERE r.guild_event_id = e.id AND r.player_id = :owner
                                  AND r.active = TRUE) AS my_rsvp,
                       e.starts_at AS sort_at
                  FROM guild_events e
                  JOIN guilds g ON g.guild_id = e.guild_id AND g.status = 'ACTIVE'
                  JOIN guild_members m ON m.guild_id = g.guild_id AND m.player_id = :owner
                 WHERE EXISTS (SELECT 1 FROM personal_role_group_links l
                                WHERE l.owner_player_id = :owner AND l.role_id = :roleId
                                  AND l.group_type = 'GUILD' AND l.group_id = g.guild_id)
                   AND (:status = 'ALL' OR e.status = :status)
                   AND e.starts_at < :to AND e.ends_at > :from
                """ + (participating ? """
                   AND EXISTS (SELECT 1 FROM guild_event_rsvps r
                                WHERE r.guild_event_id = e.id AND r.player_id = :owner
                                  AND r.active = TRUE)
                """ : "");
    }

    private static String activity(String type, boolean participating) {
        boolean party = type.equals("PARTY");
        String groupTable = party ? "parties" : "role_parties";
        String groupId = party ? "party_id" : "id";
        String ownerColumn = party ? "party_id" : "role_party_id";
        String groupName = party ? "name_original" : "name";
        String memberTable = party ? "party_members" : "role_party_members";
        String memberId = party ? "party_member_id" : "id";
        String active = party ? "" : " AND m.left_at IS NULL";
        String rsvp = "EXISTS (SELECT 1 FROM group_activity_rsvps r JOIN " + memberTable
                + " m ON m." + ownerColumn + " = g." + groupId
                + " AND m.player_id = r.player_id AND m." + memberId
                + " = r.member_id AND m.joined_at = r.member_joined_at" + active
                + " WHERE r.activity_id = e.id AND r.player_id = :owner AND r.active = TRUE)";
        return "SELECT '" + type + "_ACTIVITY' AS source_type, e.id AS source_id, NULL AS role_id,"
                + " NULL AS guild_id, NULL AS guild_name, '" + type + "' AS group_type,"
                + " g." + groupId + " AS group_id, g." + groupName + " AS group_name,"
                + " e.title AS title, e.starts_at AS starts_at, e.ends_at AS ends_at, e.status AS status,"
                + " " + rsvp + " AS my_rsvp, e.starts_at AS sort_at"
                + " FROM group_activities e JOIN " + groupTable + " g ON g." + groupId
                + " = e." + ownerColumn + " AND g.status = 'ACTIVE'"
                + " WHERE EXISTS (SELECT 1 FROM personal_role_group_links l WHERE l.owner_player_id = :owner"
                + " AND l.role_id = :roleId AND l.group_type = '" + type + "' AND l.group_id = g." + groupId + ")"
                + " AND EXISTS (SELECT 1 FROM " + memberTable + " m WHERE m." + ownerColumn
                + " = g." + groupId + " AND m.player_id = :owner" + active + ")"
                + " AND (:status = 'ALL' OR e.status = :status) AND e.starts_at < :to AND e.ends_at > :from"
                + (participating ? " AND " + rsvp : "");
    }

    private static RoleScheduleResult.Row row(ResultSet rs) throws SQLException {
        Long roleId = rs.getObject("role_id", Long.class);
        Long guildId = rs.getObject("guild_id", Long.class);
        Long groupId = rs.getObject("group_id", Long.class);
        return new RoleScheduleResult.Row(rs.getString("source_type"), rs.getLong("source_id"),
                roleId, guildId, rs.getString("guild_name"), rs.getString("group_type"),
                groupId, rs.getString("group_name"), rs.getString("title"),
                instant(rs.getTimestamp("starts_at")), instant(rs.getTimestamp("ends_at")),
                rs.getString("status"), roleId == null ? rs.getBoolean("my_rsvp") : null);
    }

    private static LocalDateTime databaseTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, DATABASE_ZONE);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toLocalDateTime().atZone(DATABASE_ZONE).toInstant();
    }
}
