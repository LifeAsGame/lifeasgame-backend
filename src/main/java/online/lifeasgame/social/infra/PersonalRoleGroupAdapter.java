package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.PersonalRoleGroupResult.*;
import online.lifeasgame.social.application.query.PersonalRoleGroupQuery;
import online.lifeasgame.social.domain.PersonalGroupType;
import online.lifeasgame.social.domain.repository.PersonalRoleGroupStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PersonalRoleGroupAdapter implements PersonalRoleGroupQuery, PersonalRoleGroupStore {
    private final NamedParameterJdbcTemplate jdbc;

    // All identifiers come exclusively from this enum switch, never request text.
    private record Source(String table, String id, String members, String memberGroup, String name) {}
    private static Source source(PersonalGroupType type) {
        return switch (type) {
            case GUILD -> new Source("guilds", "guild_id", "guild_members", "guild_id", "name_original");
            case PARTY -> new Source("parties", "party_id", "party_members", "party_id", "name_original");
            case ROLE_PARTY -> new Source("role_parties", "id", "role_party_members", "role_party_id", "name");
        };
    }

    private static String accessible(PersonalGroupType type) {
        Source s = source(type);
        String memberRole = type == PersonalGroupType.ROLE_PARTY
                ? "CASE WHEN g.leader_player_id = :owner THEN 'LEADER' ELSE 'MEMBER' END" : "m.role";
        return "SELECT '" + type.name() + "' AS group_type, g." + s.id + " AS group_id, g." + s.name
                + " AS name, " + memberRole + " AS member_role FROM " + s.table + " g JOIN " + s.members
                + " m ON m." + s.memberGroup + " = g." + s.id
                + " AND m.player_id = :owner WHERE g.status = 'ACTIVE'"
                + (type == PersonalGroupType.ROLE_PARTY ? " AND m.left_at IS NULL" : "");
    }

    @Override
    public boolean lockAccessibleGroup(Long owner, PersonalGroupType type, Long groupId) {
        Source s = source(type);
        var params = params(owner).addValue("groupId", groupId);
        // Serialize with commands locking the target aggregate; legacy membership deletion
        // may still leave an inert link. Live reads always join current membership.
        if (jdbc.queryForList("SELECT " + s.id + " FROM " + s.table + " WHERE " + s.id
                + " = :groupId FOR UPDATE", params, Long.class).isEmpty()) return false;
        return !jdbc.queryForList("SELECT group_id FROM (" + accessible(type)
                + ") a WHERE group_id = :groupId", params, Long.class).isEmpty();
    }

    @Override
    public Long add(Long owner, Long roleId, PersonalGroupType type, Long groupId) {
        var params = params(owner).addValue("roleId", roleId).addValue("type", type.name()).addValue("groupId", groupId);
        // Role lock serializes same-role additions; the unique key is the final DB boundary.
        jdbc.update("""
                INSERT INTO personal_role_group_links (owner_player_id, role_id, group_type, group_id)
                VALUES (:owner, :roleId, :type, :groupId) ON DUPLICATE KEY UPDATE id = id
                """, params);
        return jdbc.queryForObject("""
                SELECT id FROM personal_role_group_links
                WHERE owner_player_id = :owner AND role_id = :roleId AND group_type = :type AND group_id = :groupId
                """, params, Long.class);
    }

    @Override
    public void remove(Long owner, Long roleId, Long linkId) {
        jdbc.update("DELETE FROM personal_role_group_links WHERE owner_player_id = :owner AND role_id = :roleId AND id = :id",
                params(owner).addValue("roleId", roleId).addValue("id", linkId));
    }

    @Override
    public Page<Group> candidates(Long owner, PersonalGroupType type, String keyword, Pageable page) {
        var params = paging(owner, keyword, page);
        String from = " FROM (" + accessible(type) + ") g WHERE LOCATE(LOWER(:keyword), LOWER(g.name)) > 0";
        Long count = jdbc.queryForObject("SELECT COUNT(*)" + from, params, Long.class);
        var rows = jdbc.query("SELECT g.*" + from + " ORDER BY group_id DESC LIMIT :limit OFFSET :offset", params,
                (rs, n) -> group(rs));
        return new PageImpl<>(rows, page, count);
    }

    @Override
    public Page<Link> links(Long owner, Long roleId, PersonalGroupType type, String keyword, Pageable page) {
        var params = paging(owner, keyword, page).addValue("roleId", roleId).addValue("type", type == null ? null : type.name());
        String targets = type == null ? Arrays.stream(PersonalGroupType.values()).map(PersonalRoleGroupAdapter::accessible)
                .collect(Collectors.joining(" UNION ALL ")) : accessible(type);
        String from = """
                 FROM personal_role_group_links l LEFT JOIN (
                """ + targets + """
                ) g ON g.group_type = l.group_type AND g.group_id = l.group_id
                WHERE l.owner_player_id = :owner AND l.role_id = :roleId
                  AND (:type IS NULL OR l.group_type = :type)
                  AND (:keyword = '' OR LOCATE(LOWER(:keyword), LOWER(g.name)) > 0)
                """;
        Long count = jdbc.queryForObject("SELECT COUNT(*)" + from, params, Long.class);
        var rows = jdbc.query("SELECT l.id, l.role_id, l.group_type, l.group_id, g.name, g.member_role" + from
                + " ORDER BY l.id DESC LIMIT :limit OFFSET :offset", params, (rs, n) -> new Link(
                rs.getLong("id"), rs.getLong("role_id"), PersonalGroupType.valueOf(rs.getString("group_type")),
                rs.getLong("group_id"), rs.getString("name") == null ? null : group(rs)));
        return new PageImpl<>(rows, page, count);
    }

    private static Group group(ResultSet rs) throws SQLException {
        return new Group(PersonalGroupType.valueOf(rs.getString("group_type")), rs.getLong("group_id"),
                rs.getString("name"), rs.getString("member_role"));
    }
    private static MapSqlParameterSource params(Long owner) { return new MapSqlParameterSource("owner", owner); }
    private static MapSqlParameterSource paging(Long owner, String keyword, Pageable page) {
        return params(owner).addValue("keyword", keyword).addValue("limit", page.getPageSize()).addValue("offset", page.getOffset());
    }
}
