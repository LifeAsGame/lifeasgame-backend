package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCommand.Register;
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
    public PlayerResult.Created linkStart(Long userId, Register register) {
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
                playerReader.getPlayerInfo(playerId)
        );
    }
}
