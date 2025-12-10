package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.domain.*;
import online.lifeasgame.character.domain.service.LevelingPolicy;
import online.lifeasgame.core.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerWriter playerWriter;
    private final PlayerReader playerReader;
    private final PlayerTitleReader playerTitleReader;
    private final LevelingPolicy levelingPolicy;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public PlayerResult.Created linkStart(Long userId, PlayerCommand.Register register) {
        playerReader.assertNotExistsByUserId(userId);

        Player player = Player.linkStart(
                userId,
                Name.of(register.name()),
                GenderType.parse(register.gender())
        );

        playerWriter.create(player);

        domainEventPublisher.publishAll(player.pullEvents());

        return new PlayerResult.Created(player.getId());
    }

    public PlayerResult.PlayerInfo getPlayerInfo(Long playerId) {
        Player player = playerReader.getByIdOrThrow(playerId);
        return PlayerResult.PlayerInfo.from(player);
    }

    @Transactional
    public PlayerResult.UpdatedTitle changeRepresentativeTitle(Long playerId, Long titleId) {
        playerTitleReader.assertHasTitle(playerId, titleId);

        Player player = playerReader.getByIdOrThrow(playerId);
        player.changeRepresentativeTitle(titleId);

        return new PlayerResult.UpdatedTitle(player.getTitleId());
    }

    @Transactional
    public PlayerResult.ExpGranted grantExp(Long playerId, long exp) {
        Player player = playerReader.getByIdOrThrow(playerId);

        Player.GainResult gainResult = player.gainExp(exp, levelingPolicy);

        domainEventPublisher.publishAll(player.pullEvents());

        return PlayerResult.ExpGranted.from(playerId, gainResult);
    }

    @Transactional
    public PlayerResult.CurrentHp adjustHp(PlayerCommand.ChangeHp command) {
        Player player = playerReader.getByIdOrThrow(command.playerId());
        player.adjustHp(command.hpDelta());
        return PlayerResult.CurrentHp.from(player);
    }

    @Transactional
    public PlayerResult.HpCapacity adjustHpCapacity(PlayerCommand.ChangeHpCapacity command) {
        Player player = playerReader.getByIdOrThrow(command.playerId());
        player.adjustHpCapacity(command.hpCapacityDelta());
        return PlayerResult.HpCapacity.from(player);
    }

    @Transactional
    public PlayerResult.CurrentMp adjustMp(PlayerCommand.ChangeMp command) {
        Player player = playerReader.getByIdOrThrow(command.playerId());
        player.adjustMana(command.mpDelta());
        return PlayerResult.CurrentMp.from(player);
    }

    @Transactional
    public PlayerResult.MpCapacity adjustMpCapacity(PlayerCommand.ChangeMpCapacity command) {
        Player player = playerReader.getByIdOrThrow(command.playerId());
        player.adjustManaCapacity(command.mpCapacityDelta());
        return PlayerResult.MpCapacity.from(player);
    }

    @Transactional
    public PlayerResult.CoreStatsGranted grantCoreStats(PlayerCommand.GrantCoreStats command) {
        Player player = playerReader.getByIdOrThrow(command.playerId());
        player.grantCoreStats(
                CoreStatDelta.of(
                        command.strDelta(),
                        command.agiDelta(),
                        command.dexDelta(),
                        command.intelDelta(),
                        command.vitDelta(),
                        command.lucDelta()
                )
        );

        return PlayerResult.CoreStatsGranted.from(player.getId(), player.getStats());
    }

    @Transactional
    public PlayerResult.StatusEffectsGranted grantStatusEffects(PlayerCommand.GrantStatusEffects command) {
        Player player = playerReader.getByIdOrThrow(command.playerId());
        player.applyStatusEffects(
                StatusEffects.of(
                        command.codes().stream()
                                .map(StatusEffectCode::parse)
                                .toList()
                )
        );

        return PlayerResult.StatusEffectsGranted.from(player.getId(), player.getStatusEffects());
    }
}
