package online.lifeasgame.role.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.application.query.PersonRoleContextQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PersonRoleContextQueryAdapter implements PersonRoleContextQuery {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Page<Context> findOwned(Long playerId, Long personId, boolean includeArchived, String keyword, Pageable page) {
        var parameters = new MapSqlParameterSource().addValue("owner", playerId).addValue("person", personId)
                .addValue("history", includeArchived).addValue("keyword", keyword)
                .addValue("limit", page.getPageSize()).addValue("offset", page.getOffset());
        String from = """
                FROM role_relations rr JOIN roles r ON r.id = rr.role_id AND r.player_id = rr.player_id
                WHERE rr.player_id = :owner AND rr.person_id = :person
                  AND (:history OR (r.status = 'ACTIVE' AND rr.status = 'ACTIVE'))
                  AND LOCATE(LOWER(:keyword), LOWER(r.name)) > 0
                """;
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + from, parameters, Long.class);
        var rows = jdbc.query("""
                SELECT rr.id, rr.role_id, rr.person_id, r.name, r.status AS role_status,
                       rr.relation_type, rr.role_notes, rr.status, rr.version
                """ + from + " ORDER BY rr.id ASC LIMIT :limit OFFSET :offset", parameters,
                (rs, n) -> new Context(rs.getLong("id"), rs.getLong("role_id"), rs.getLong("person_id"),
                        rs.getString("name"), rs.getString("role_status"), rs.getString("relation_type"),
                        rs.getString("role_notes"), rs.getString("status"), rs.getLong("version")));
        return new PageImpl<>(rows, page, total);
    }
}
