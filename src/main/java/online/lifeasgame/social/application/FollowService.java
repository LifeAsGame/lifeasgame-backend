package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.result.FollowResult;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.FollowState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class FollowService {

    private final FollowReader followReader;
    private final FollowRegistrar followRegistrar;
    private final FollowTargetVerifier followTargetVerifier;

    @Transactional
    public FollowResult.Info follow(Long playerId, FollowCommand.Create command) {
        Long targetPlayerId = command.targetPlayerId();
        followTargetVerifier.verifyExists(targetPlayerId);
        Follow follow = followReader.findByPlayerIdAndTargetPlayerId(
                playerId,
                targetPlayerId
        ).orElseGet(() -> followRegistrar.register(
                Follow.create(
                        playerId,
                        targetPlayerId
                )
        ));
        if (follow.getState() == FollowState.STOPPED) {
            follow.followAgain();
        }
        
        return FollowResult.Info.from(follow);
    }

    @Transactional
    public void unfollow(Long playerId, Long followId) {
        Follow follow = followReader.getByFollowIdAndPlayerId(followId, playerId);
        follow.unfollow();
    }

    @Transactional
    public void mute(Long playerId, Long followId) {
        Follow follow = followReader.getByFollowIdAndPlayerId(followId, playerId);
        follow.mute();
    }

    @Transactional
    public void unmute(Long playerId, Long followId) {
        Follow follow = followReader.getByFollowIdAndPlayerId(followId, playerId);
        follow.unmute();
    }

    @Transactional
    public void block(Long playerId, Long followId) {
        Follow follow = followReader.getByFollowIdAndPlayerId(followId, playerId);
        follow.block();
    }

    @Transactional
    public void unblock(Long playerId, Long followId) {
        Follow follow = followReader.getByFollowIdAndPlayerId(followId, playerId);
        follow.unblock();
    }

    public FollowResult.Page<FollowResult.Summary> listFollowings(Long playerId, int page, int size) {
        List<Follow> followings = followReader.getFollowingsByPlayerId(playerId, page, size);
        long total = followReader.countFollowings(playerId);
        List<FollowResult.Summary> contents = followings.stream().map(FollowResult.Summary::from).toList();
        return FollowResult.Page.of(contents, page, size, total);
    }

    public FollowResult.Page<FollowResult.Summary> listFollowers(Long playerId, int page, int size) {
        List<Follow> followers = followReader.getFollowersByPlayerId(playerId, page, size);
        long total = followReader.countFollowers(playerId);
        List<FollowResult.Summary> contents = followers.stream().map(FollowResult.Summary::from).toList();
        return FollowResult.Page.of(contents, page, size, total);
    }

    public List<FollowResult.Summary> recentFollowings(Long playerId, int limit) {
        return followReader.recentFollowings(playerId, limit).stream().map(FollowResult.Summary::from).toList();
    }

    public List<FollowResult.Summary> recentFollowers(Long playerId, int limit) {
        return followReader.recentFollowers(playerId, limit).stream().map(FollowResult.Summary::from).toList();
    }
}
