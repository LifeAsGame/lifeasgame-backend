package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AdminPlayerCommand;
import online.lifeasgame.character.application.result.AdminPlayerResult;
import online.lifeasgame.character.domain.CoreStatDelta;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.Player.GainResult;
import online.lifeasgame.character.domain.StatusEffectCode;
import online.lifeasgame.character.domain.StatusEffects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminPlayerService {

    private final PlayerWriter playerWriter;

    @Transactional
    public AdminPlayerResult.ExpGranted grantExp(Long playerId, long exp) {
        GainResult gainResult = playerWriter.grantExp(playerId, exp);
        return AdminPlayerResult.ExpGranted.of(
                playerId,
                gainResult
        );
    }

    @Transactional
    public AdminPlayerResult.CoreStatsGranted grantCoreStats(AdminPlayerCommand.GrantCoreStats command) {
        Player player = playerWriter.grantCoreStats(
                command.playerId(),
                CoreStatDelta.of(
                        command.strDelta(),
                        command.agiDelta(),
                        command.dexDelta(),
                        command.intelDelta(),
                        command.vitDelta(),
                        command.lucDelta()
                )
        );
        return AdminPlayerResult.CoreStatsGranted.of(
                player.getId(),
                player.getStats()
        );
    }

    @Transactional
    public AdminPlayerResult.StatusEffectsGranted grantStatusEffects(AdminPlayerCommand.GrantStatusEffects command) {
        Player player = playerWriter.applyStatusEffects(
                command.playerId(),
                StatusEffects.of(
                        command.codes().stream()
                                .map(StatusEffectCode::parse)
                                .toList()
                )
        );
        return AdminPlayerResult.StatusEffectsGranted.from(
                player.getId(),
                player.getStatusEffects()
        );
    }
}
