package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.growth.PlayerGrowthChangeReader;
import online.lifeasgame.character.application.growth.PlayerGrowthChangeWriter;
import online.lifeasgame.character.application.internal.PlayerGrowthApi;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.growth.PlayerGrowthChange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerRewardExpGrantService implements PlayerGrowthApi {

    private final PlayerReader playerReader;
    private final PlayerExpGrantService playerExpGrantService;
    private final PlayerGrowthChangeReader growthChangeReader;
    private final PlayerGrowthChangeWriter growthChangeWriter;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PlayerGrowthGrantResult grantRewardExp(
            Long playerId,
            Long rewardLineId,
            long amount
    ) {
        Player player = playerReader.getByIdForUpdateOrThrow(playerId);
        Optional<PlayerGrowthChange> existing = growthChangeReader.findByRewardLineId(rewardLineId);
        if (existing.isPresent()) {
            PlayerGrowthChange change = existing.get();
            change.assertMatches(playerId, rewardLineId, amount);
            return toResult(change, true);
        }

        Player.GainResult gainResult = playerExpGrantService.grantExp(player, amount);
        PlayerGrowthChange change = PlayerGrowthChange.rewardExp(playerId, rewardLineId, gainResult);
        PlayerGrowthChange saved = growthChangeWriter.saveAndFlush(change);
        return toResult(saved, false);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public Optional<PlayerGrowthGrantResult> findRewardExpGrant(Long rewardLineId) {
        return growthChangeReader.findByRewardLineId(rewardLineId)
                .map(change -> toResult(change, true));
    }

    private PlayerGrowthGrantResult toResult(PlayerGrowthChange change, boolean replayed) {
        return new PlayerGrowthGrantResult(
                change.getId(),
                change.getPlayerId(),
                change.getRewardLineId(),
                change.getRequestedExp(),
                change.getAppliedExp(),
                change.getLeftoverExp(),
                change.getBeforeLevel(),
                change.getAfterLevel(),
                change.getBeforeTotalExp(),
                change.getAfterTotalExp(),
                replayed
        );
    }
}
