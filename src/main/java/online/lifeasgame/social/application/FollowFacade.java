package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.result.FollowResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FollowFacade {
    
    private final FollowService followService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public FollowResult.Info follow(FollowCommand.Create command) {
        Long playerId = getPlayer();
        return followService.follow(playerId, command);
    }

    public void unfollow(Long followId, FollowCommand.Unfollow command) {
        Long playerId = getPlayer();
        followService.unfollow(playerId, followId, command);
    }

    public void mute(Long followId, FollowCommand.Mute command) {
        Long playerId = getPlayer();
        followService.mute(playerId, followId, command);
    }

    public void unmute(Long followId, FollowCommand.Unmute command) {
        Long playerId = getPlayer();
        followService.unmute(playerId, followId, command);
    }

    public void block(Long followId, FollowCommand.Block command) {
        Long playerId = getPlayer();
        followService.block(playerId, followId, command);
    }

    public void unblock(Long followId, FollowCommand.Unblock command) {
        Long playerId = getPlayer();
        followService.unblock(playerId, followId, command);
    }

    public FollowResult.Page<FollowResult.Summary> listFollowings(int page, int size) {
        Long playerId = getPlayer();
        return followService.listFollowings(playerId, page, size);
    }

    public FollowResult.Page<FollowResult.Summary> listFollowers(int page, int size) {
        Long playerId = getPlayer();
        return followService.listFollowers(playerId, page, size);
    }

    public List<FollowResult.Summary> recentFollowings(int limit) {
        Long playerId = getPlayer();
        return followService.recentFollowings(playerId, limit);
    }

    public List<FollowResult.Summary> recentFollowers(int limit) {
        Long playerId = getPlayer();
        return followService.recentFollowers(playerId, limit);
    }

    private Long getPlayer() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }
}
