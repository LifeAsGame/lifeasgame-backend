package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.MemberAccess;
import online.lifeasgame.social.domain.PersonalGroupType;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberAccessSql implements MemberAccess {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void requirePair(PersonalGroupType type, Long groupId, Long actorId, Long memberId, boolean lock) {
        if (type == null || groupId == null || groupId <= 0 || memberId == null || memberId <= 0)
            throw new DomainException(SocialError.PERSONAL_GROUP_INVALID_INPUT);
        Source s = source(type);
        var p = new MapSqlParameterSource("groupId", groupId).addValue("actor", actorId).addValue("member", memberId);
        if (lock && jdbc.queryForList("SELECT " + s.id + " FROM " + s.group + " WHERE " + s.id
                + " = :groupId FOR UPDATE", p, Long.class).isEmpty()) throw missing(type);
        String from = " FROM " + s.group + " g JOIN " + s.members
                + " a ON a." + s.memberGroup + " = g." + s.id + " AND a.player_id = :actor"
                + " JOIN " + s.members + " t ON t." + s.memberGroup + " = g." + s.id
                + " AND t.player_id = :member WHERE g." + s.id + " = :groupId AND g.status = 'ACTIVE'"
                + s.activeMembers;
        boolean present = lock
                ? !jdbc.queryForList("SELECT a." + s.memberPk + from + " FOR UPDATE", p, Long.class).isEmpty()
                : Boolean.TRUE.equals(jdbc.queryForObject("SELECT COUNT(*) > 0" + from, p, Boolean.class));
        if (!present) throw missing(type);
    }

    private static DomainException missing(PersonalGroupType type) {
        return new DomainException(switch (type) {
            case GUILD -> SocialError.GUILD_NOT_FOUND;
            case PARTY -> SocialError.PARTY_NOT_FOUND;
            case ROLE_PARTY -> SocialError.ROLE_PARTY_NOT_FOUND;
        });
    }

    private static Source source(PersonalGroupType type) {
        return switch (type) {
            case GUILD -> new Source("guilds", "guild_id", "guild_members", "guild_id", "guild_member_id", "");
            case PARTY -> new Source("parties", "party_id", "party_members", "party_id", "party_member_id", "");
            case ROLE_PARTY -> new Source("role_parties", "id", "role_party_members", "role_party_id", "id",
                    " AND a.left_at IS NULL AND t.left_at IS NULL");
        };
    }
    private record Source(String group, String id, String members, String memberGroup,
                          String memberPk, String activeMembers) {}
}
