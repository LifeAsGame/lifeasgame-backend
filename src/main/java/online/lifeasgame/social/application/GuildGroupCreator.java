package online.lifeasgame.social.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildStatus;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.GuildRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuildGroupCreator {
    private final GuildRepository guilds;
    private final PartyService parties;
    private final RolePartyService roleParties;
    private final GuildGroupLinkService links;
    private final CurrentPlayerAccessor currentPlayer;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public record RolePartyDetails(Long roleId, String name, String description, int maxMembers) {}
    public record Command(UUID clientRequestId, String groupType, String displayName,
                          PartyCommand.Create party, RolePartyDetails roleParty) {}
    public record Capabilities(boolean canOpenGroup, boolean canManageGroup) {}
    public record Result(String groupType, Long groupId, Long linkId, String linkStatus,
                         Capabilities capabilities, @JsonIgnore boolean replayed) {}

    @Transactional
    public Result create(Long guildId, Command command) {
        Long actor = currentPlayer.currentPlayerIdOrThrow();
        if (command == null || command.clientRequestId() == null || command.displayName() == null ||
                !("PARTY".equals(command.groupType()) && command.party() != null && command.roleParty() == null ||
                  "ROLE_PARTY".equals(command.groupType()) && command.roleParty() != null && command.party() == null)) {
            throw new DomainException(SocialError.GUILD_GROUP_INVALID_INPUT);
        }
        Guild guild = guilds.findForUpdate(guildId).orElseThrow(() -> new DomainException(SocialError.GUILD_NOT_FOUND));
        if (guild.getStatus() != GuildStatus.ACTIVE || guild.findMember(actor).isEmpty())
            throw new DomainException(SocialError.GUILD_NOT_FOUND);
        String hash = hash(command);
        var p = new MapSqlParameterSource("guild", guildId).addValue("actor", actor)
                .addValue("key", command.clientRequestId().toString());
        var previous = jdbc.query("SELECT request_hash, group_type, group_id, link_id, link_status FROM guild_group_creation_receipts "
                        + "WHERE guild_id=:guild AND actor_player_id=:actor AND client_request_id=:key", p,
                (rs, n) -> new Receipt(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getString(5)))
                .stream().findFirst().orElse(null);
        if (previous != null) {
            if (!previous.hash().equals(hash)) throw new DomainException(SocialError.GUILD_GROUP_CONFLICT);
            return result(previous.type(), previous.groupId(), previous.linkId(), previous.status(), true);
        }
        Long groupId;
        if ("PARTY".equals(command.groupType())) {
            groupId = parties.create(actor, command.party()).id();
        } else {
            RolePartyDetails d = command.roleParty();
            if (d.roleId() == null || d.roleId() <= 0) throw new DomainException(SocialError.GUILD_GROUP_INVALID_INPUT);
            groupId = roleParties.create(d.roleId(), d.name(), d.description(), d.maxMembers()).id();
        }
        var link = links.propose(guildId, command.groupType(), groupId, command.displayName());
        jdbc.update("INSERT INTO guild_group_creation_receipts "
                        + "(guild_id,actor_player_id,client_request_id,request_hash,group_type,group_id,link_id,link_status) "
                        + "VALUES (:guild,:actor,:key,:hash,:type,:groupId,:linkId,:status)",
                p.addValue("hash", hash).addValue("type", command.groupType())
                        .addValue("groupId", groupId).addValue("linkId", link.id()).addValue("status", link.status()));
        return result(command.groupType(), groupId, link.id(), link.status(), false);
    }

    private static Result result(String type, Long groupId, Long linkId, String status, boolean replayed) {
        return new Result(type, groupId, linkId, status,
                new Capabilities(true, true), replayed);
    }

    private String hash(Command command) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(command));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | JsonProcessingException ex) {
            throw new IllegalStateException("Unable to hash group creation request", ex);
        }
    }

    private record Receipt(String hash, String type, Long groupId, Long linkId, String status) {}
}
