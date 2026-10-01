package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildMember;
import online.lifeasgame.social.domain.GuildWaitMember;
import online.lifeasgame.social.domain.GuildWaitType;
import online.lifeasgame.social.domain.GuildVisibility;
import online.lifeasgame.social.domain.repository.GuildRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class GuildRepositoryAdapter implements GuildRepository {

    private final GuildJpaRepository jpaRepository;

    @Override
    public Guild save(Guild guild) {
        return jpaRepository.save(guild);
    }

    @Override
    public Optional<Guild> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Guild> findByIdAndPlayerId(Long id, Long playerId) {
        return jpaRepository.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public List<Guild> search(String keyword, GuildVisibility visibility, int page, int size) {
        Page<Long> idPage = jpaRepository.searchIds(
                keyword,
                visibility,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        if (idPage.isEmpty()) return List.of();
        List<Long> ids = idPage.getContent();
        List<Guild> list = jpaRepository.fetchWithTagsByIds(ids);

        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        list.sort(Comparator.comparingInt(g -> order.getOrDefault(g.getId(), Integer.MAX_VALUE)));
        return list;
    }

    @Override
    public long countSearch(String keyword, GuildVisibility visibility) {
        return jpaRepository.searchIds(keyword, visibility, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<Guild> recent(int limit) {
        List<Long> ids = jpaRepository.findRecent(PageRequest.of(0, limit));
        if (ids.isEmpty()) return List.of();
        List<Guild> rows = jpaRepository.findRecentWithTags(ids);
        rows.sort(Comparator.comparingInt(g -> ids.indexOf(g.getId())));
        return rows;
    }
    @Override
    public Page<Guild> findMine(Long playerId, Pageable pageable) {
        return jpaRepository.findMine(playerId, pageable);
    }

    @Override
    public Page<GuildMember> findMembers(Long guildId, Pageable pageable) {
        return jpaRepository.findMembers(guildId, pageable);
    }

    @Override
    public Page<GuildWaitMember> findPendingRequests(Long guildId, Pageable pageable) {
        return jpaRepository.findPendingRequests(guildId, pageable);
    }

    @Override
    public Page<GuildWaitMember> findMyPending(Long playerId, GuildWaitType type, Pageable pageable) {
        return jpaRepository.findMyPending(playerId, type, pageable);
    }
}
