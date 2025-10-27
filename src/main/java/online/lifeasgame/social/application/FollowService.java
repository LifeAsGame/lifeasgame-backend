package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.model.FollowSpec;
import online.lifeasgame.social.application.result.FollowResult;
import online.lifeasgame.social.domain.Follow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class FollowService {

    private final FollowReader followReader;
    private final FollowWriter followWriter;

    @Transactional
    public FollowResult.Info follow(Long playerId, FollowCommand.Create command) {
        Follow saved = followWriter.create(FollowSpec.Create.from(playerId, command));
        return FollowResult.Info.from(saved);
    }

    @Transactional
    public void unfollow(Long playerId, Long followId) {
        Follow f = followReader.get(followId, playerId);
        followWriter.unfollow(f);
    }

    @Transactional
    public void mute(Long playerId, Long followId) {
        Follow f = followReader.get(followId, playerId);
        followWriter.mute(f);
    }

    @Transactional
    public void unmute(Long playerId, Long followId) {
        Follow f = followReader.get(followId, playerId);
        followWriter.unmute(f);
    }

    @Transactional
    public void block(Long playerId, Long followId) {
        Follow f = followReader.get(followId, playerId);
        followWriter.block(f);
    }

    @Transactional
    public void unblock(Long playerId, Long followId) {
        Follow f = followReader.get(followId, playerId);
        followWriter.unblock(f);
    }

    public FollowResult.Page<FollowResult.Summary> listFollowings(Long playerId, int page, int size) {
        List<Follow> domains = followReader.followings(playerId, page, size);
        long total = followReader.countFollowings(playerId);
        List<FollowResult.Summary> contents = domains.stream().map(FollowResult.Summary::from).toList();
        return FollowResult.Page.of(contents, page, size, total);
    }

    public FollowResult.Page<FollowResult.Summary> listFollowers(Long playerId, int page, int size) {
        List<Follow> domains = followReader.followers(playerId, page, size);
        long total = followReader.countFollowers(playerId);
        List<FollowResult.Summary> contents = domains.stream().map(FollowResult.Summary::from).toList();
        return FollowResult.Page.of(contents, page, size, total);
    }

    public List<FollowResult.Summary> recentFollowings(Long playerId, int limit) {
        return followReader.recentFollowings(playerId, limit).stream().map(FollowResult.Summary::from).toList();
    }

    public List<FollowResult.Summary> recentFollowers(Long playerId, int limit) {
        return followReader.recentFollowers(playerId, limit).stream().map(FollowResult.Summary::from).toList();
    }
}
