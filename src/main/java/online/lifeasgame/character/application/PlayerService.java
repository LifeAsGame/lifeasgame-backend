package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerWriter playerWriter;
    private final PlayerReader playerReader;

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
}
