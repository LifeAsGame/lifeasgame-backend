package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AdminPlayerCommand;
import online.lifeasgame.character.application.result.AdminPlayerResult;
import online.lifeasgame.character.domain.CoreStatDelta;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.Player.GainResult;
import online.lifeasgame.character.domain.StatusEffectCode;
import online.lifeasgame.character.domain.StatusEffects;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class AdminPlayerService {

    private final PlayerWriter playerWriter;
    private final PlayerTitleReader playerTitleReader;

    @Transactional
    public AdminPlayerResult.ExpGranted grantExp(Long playerId, long exp) {
        GainResult gainResult = playerWriter.grantExp(playerId, exp);
        return AdminPlayerResult.ExpGranted.of(
                playerId,
                gainResult
        );
    }

    @Transactional
    public AdminPlayerResult.CurrentHp changeHp(AdminPlayerCommand.ChangeHp command) {
        return AdminPlayerResult.CurrentHp.from(
                playerWriter.changeHp(command.playerId(), command.hpDelta())
        );
    }

    @Transactional
    public AdminPlayerResult.HpCapacity changeHpCapacity(AdminPlayerCommand.ChangeHpCapacity command) {
        return AdminPlayerResult.HpCapacity.from(
                playerWriter.changeHpCapacity(command.playerId(), command.hpCapacityDelta())
        );
    }

    @Transactional
    public AdminPlayerResult.CurrentMp changeMp(AdminPlayerCommand.ChangeMp command) {
        return AdminPlayerResult.CurrentMp.from(
                playerWriter.changeMp(command.playerId(), command.mpDelta())
        );
    }

    @Transactional
    public AdminPlayerResult.MpCapacity changeMpCapacity(AdminPlayerCommand.ChangeMpCapacity command) {
        return AdminPlayerResult.MpCapacity.from(
                playerWriter.changeMpCapacity(command.playerId(), command.mpCapacityDelta())
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

    @Transactional
    public AdminPlayerResult.UpdatedTitle changeRepresentativeTitle(Long playerId, Long titleId) {
        if (!playerTitleReader.hasTitle(playerId, titleId)) {
            throw new DomainException(PlayerError.INVALID_TITLE);
        }

        return AdminPlayerResult.UpdatedTitle.of(
                playerWriter.changeRepresentativeTitle(playerId, titleId)
        );
    }
}
