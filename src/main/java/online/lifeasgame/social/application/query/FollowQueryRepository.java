package online.lifeasgame.social.application.query;

import online.lifeasgame.social.domain.Follow;

import java.util.List;

public interface FollowQueryRepository {
    // 내가 팔로우하는 목록
    List<Follow> findFollowings(Long playerId, int page, int size);

    long countFollowings(Long playerId);

    // 나를 팔로우하는 목록
    List<Follow> findFollowers(Long playerId, int page, int size);

    long countFollowers(Long playerId);

    // 최근
    List<Follow> recentFollowings(Long playerId, int limit);

    List<Follow> recentFollowers(Long playerId, int limit);
}
