package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.repository.FollowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class FollowRepositoryAdapter implements FollowRepository {

    private final FollowJpaRepository followJpaRepository;

    @Override
    public Follow save(Follow f) {
        return followJpaRepository.save(f);
    }

    @Override
    public Optional<Follow> findById(Long id) {
        return followJpaRepository.findById(id);
    }

    @Override
    public Optional<Follow> findByIdAndPlayerId(Long id, Long playerId) {
        return followJpaRepository.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public boolean existsByPlayerIdAndTargetId(Long playerId, Long friendId) {
        return followJpaRepository.existsByPlayerIdAndTargetPlayerId(playerId, friendId);
    }

    // QueryRepo — 2단계 로딩
    @Override
    public List<Follow> findFollowings(Long playerId, int page, int size) {
        Page<Long> idPage = followJpaRepository.findFollowingIds(
                playerId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        if (idPage.isEmpty()) return List.of();
        List<Long> ids = idPage.getContent();
        List<Follow> list = followJpaRepository.findAllByIdIn(ids);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        list.sort(Comparator.comparingInt(f -> order.getOrDefault(f.getId(), Integer.MAX_VALUE)));
        return list;
    }

    @Override
    public long countFollowings(Long playerId) {
        return followJpaRepository.findFollowingIds(playerId, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<Follow> findFollowers(Long playerId, int page, int size) {
        Page<Long> idPage = followJpaRepository.findFollowerIds(
                playerId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        if (idPage.isEmpty()) return List.of();
        List<Long> ids = idPage.getContent();
        List<Follow> list = followJpaRepository.findAllByIdIn(ids);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        list.sort(Comparator.comparingInt(f -> order.getOrDefault(f.getId(), Integer.MAX_VALUE)));
        return list;
    }

    @Override
    public long countFollowers(Long playerId) {
        return followJpaRepository.countByPlayerId(playerId);
    }

    @Override
    public List<Follow> recentFollowings(Long playerId, int limit) {
        return followJpaRepository.findRecentFollowings(playerId, PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));
    }

    @Override
    public List<Follow> recentFollowers(Long playerId, int limit) {
        return followJpaRepository.findRecentFollowers(playerId, PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));
    }
}
