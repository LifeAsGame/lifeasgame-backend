package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildVisibility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import online.lifeasgame.social.domain.GuildMember;
import online.lifeasgame.social.domain.GuildWaitMember;
import online.lifeasgame.social.domain.GuildWaitType;
import online.lifeasgame.social.domain.GuildStatus;
import online.lifeasgame.social.domain.GuildMemberRole;

import java.util.List;
import java.util.Optional;

public interface GuildRepository {
    Guild save(Guild g);

    Optional<Guild> findById(Long id);

    Optional<Guild> findByIdAndPlayerId(Long id, Long playerId);

    List<Guild> search(String keyword, GuildVisibility visibility, int page, int size);

    long countSearch(String keyword, GuildVisibility visibility);

    List<Guild> recent(int limit);

    Page<MyGuild> findMine(Long playerId, Pageable pageable);
    Page<GuildMember> findMembers(Long guildId, Pageable pageable);
    Page<GuildWaitMember> findPendingRequests(Long guildId, Pageable pageable);
    Page<GuildWaitMember> findMyPending(Long playerId, GuildWaitType type, Pageable pageable);
    record MyGuild(Long id, String name, String code, GuildStatus status,
                    GuildMemberRole role, long memberCount, int maxMembers) {}
}
