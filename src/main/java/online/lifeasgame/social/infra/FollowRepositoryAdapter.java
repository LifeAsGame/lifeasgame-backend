package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.FollowState;
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
    public Follow save(Follow follow) {
        return followJpaRepository.save(follow);
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
    public Optional<Follow> findByPlayerIdAndTargetPlayerId(Long playerId, Long targetPlayerId) {
        return followJpaRepository.findByPlayerIdAndTargetPlayerId(playerId, targetPlayerId);
    }

    @Override
    public boolean existsActiveFollow(Long playerId, Long targetPlayerId) {
        return followJpaRepository.existsByPlayerIdAndTargetPlayerIdAndState(
                playerId,
                targetPlayerId,
                FollowState.FOLLOWING
        );
    }

    @Override
    public List<Follow> findFollowings(Long playerId, int page, int size) {
        Page<Long> idPage = followJpaRepository.findFollowingIds(
                playerId,
                FollowState.FOLLOWING,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        if (idPage.isEmpty()) return List.of();
        List<Long> ids = idPage.getContent();
        List<Follow> list = followJpaRepository.findAllByIdInAndState(ids, FollowState.FOLLOWING);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        list.sort(Comparator.comparingInt(f -> order.getOrDefault(f.getId(), Integer.MAX_VALUE)));
        return list;
    }

    @Override
    public long countFollowings(Long playerId) {
        return followJpaRepository.countByPlayerIdAndState(playerId, FollowState.FOLLOWING);
    }

    @Override
    public List<Follow> findFollowers(Long playerId, int page, int size) {
        Page<Long> idPage = followJpaRepository.findFollowerIds(
                playerId,
                FollowState.FOLLOWING,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        if (idPage.isEmpty()) return List.of();
        List<Long> ids = idPage.getContent();
        List<Follow> list = followJpaRepository.findAllByIdInAndState(ids, FollowState.FOLLOWING);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        list.sort(Comparator.comparingInt(f -> order.getOrDefault(f.getId(), Integer.MAX_VALUE)));
        return list;
    }

    @Override
    public long countFollowers(Long playerId) {
        return followJpaRepository.countByTargetPlayerIdAndState(playerId, FollowState.FOLLOWING);
    }

    @Override
    public List<Follow> recentFollowings(Long playerId, int limit) {
        return followJpaRepository.findRecentFollowings(
                playerId,
                FollowState.FOLLOWING,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 100))
        );
    }

    @Override
    public List<Follow> recentFollowers(Long playerId, int limit) {
        return followJpaRepository.findRecentFollowers(
                playerId,
                FollowState.FOLLOWING,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 100))
        );
    }
}
