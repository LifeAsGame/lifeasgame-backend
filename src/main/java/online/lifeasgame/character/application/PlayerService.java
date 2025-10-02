package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.domain.*;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerWriter playerWriter;
    private final PlayerReader playerReader;
    private final PlayerTitleReader playerTitleReader;

    @Transactional
    public PlayerResult.Created linkStart(Long userId, PlayerCommand.Register register) {
        Player player = Player.linkStart(
                userId,
                Name.of(register.name()),
                GenderType.parse(register.gender())
        );

        return PlayerResult.Created.of(
                playerWriter.register(player)
        );
    }

    public PlayerResult.PlayerInfo getPlayerInfo(Long playerId) {
        return PlayerResult.PlayerInfo.from(
                playerReader.getPlayer(playerId)
        );
    }

    @Transactional
    public PlayerResult.UpdatedTitle changeRepresentativeTitle(Long playerId, Long titleId) {
        boolean owned = playerTitleReader.hasTitle(playerId, titleId);
        if (!owned) {
            throw new DomainException(PlayerError.INVALID_TITLE);
        }

        return PlayerResult.UpdatedTitle.of(
                playerWriter.changeRepresentativeTitle(playerId, titleId)
        );
    }

    @Transactional
    public PlayerResult.ExpGranted grantExp(Long playerId, long exp) {
        Player.GainResult gainResult = playerWriter.grantExp(playerId, exp);
        return PlayerResult.ExpGranted.of(
                playerId,
                gainResult
        );
    }

    @Transactional
    public PlayerResult.CurrentHp changeHp(PlayerCommand.ChangeHp command) {
        return PlayerResult.CurrentHp.from(
                playerWriter.changeHp(command.playerId(), command.hpDelta())
        );
    }

    @Transactional
    public PlayerResult.HpCapacity changeHpCapacity(PlayerCommand.ChangeHpCapacity command) {
        return PlayerResult.HpCapacity.from(
                playerWriter.changeHpCapacity(command.playerId(), command.hpCapacityDelta())
        );
    }

    @Transactional
    public PlayerResult.CurrentMp changeMp(PlayerCommand.ChangeMp command) {
        return PlayerResult.CurrentMp.from(
                playerWriter.changeMp(command.playerId(), command.mpDelta())
        );
    }

    @Transactional
    public PlayerResult.MpCapacity changeMpCapacity(PlayerCommand.ChangeMpCapacity command) {
        return PlayerResult.MpCapacity.from(
                playerWriter.changeMpCapacity(command.playerId(), command.mpCapacityDelta())
        );
    }

    @Transactional
    public PlayerResult.CoreStatsGranted grantCoreStats(PlayerCommand.GrantCoreStats command) {
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
        return PlayerResult.CoreStatsGranted.of(
                player.getId(),
                player.getStats()
        );
    }

    @Transactional
    public PlayerResult.StatusEffectsGranted grantStatusEffects(PlayerCommand.GrantStatusEffects command) {
        Player player = playerWriter.applyStatusEffects(
                command.playerId(),
                StatusEffects.of(
                        command.codes().stream()
                                .map(StatusEffectCode::parse)
                                .toList()
                )
        );
        return PlayerResult.StatusEffectsGranted.from(
                player.getId(),
                player.getStatusEffects()
        );
    }
}
