package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
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
}
