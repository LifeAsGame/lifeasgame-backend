package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.query.GuildQueryRepository;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildVisibility;
import online.lifeasgame.social.domain.repository.GuildRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class GuildRepositoryAdapter implements GuildRepository, GuildQueryRepository {

    private final GuildJpaRepository guildJpaRepository;

    @Override
    public Guild save(Guild g) {
        return guildJpaRepository.save(g);
    }

    @Override
    public Optional<Guild> findById(Long id) {
        return guildJpaRepository.findById(id);
    }

    @Override
    public Optional<Guild> findByIdAndPlayerId(Long id, Long playerId) {
        return guildJpaRepository.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public List<Guild> search(String keyword, GuildVisibility visibility, int page, int size) {
        Page<Long> idPage = guildJpaRepository.searchIds(
                keyword,
                visibility,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        if (idPage.isEmpty()) return List.of();
        List<Long> ids = idPage.getContent();
        List<Guild> list = guildJpaRepository.fetchWithTagsByIds(ids);

        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        list.sort(Comparator.comparingInt(g -> order.getOrDefault(g.getId(), Integer.MAX_VALUE)));
        return list;
    }

    @Override
    public long countSearch(String keyword, GuildVisibility visibility) {
        return guildJpaRepository.searchIds(keyword, visibility, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<Guild> recent(int limit) {
        return guildJpaRepository.findRecentWithTags(PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));
    }
}
