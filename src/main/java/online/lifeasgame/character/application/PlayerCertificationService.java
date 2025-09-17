package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.domain.PlayerCertification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerCertificationService {

    private final PlayerCertificationWriter playerCertificationWriter;
    private final PlayerCertificationReader playerCertificationReader;

    public List<PlayerCertificationResult.PlayerCertificationInfo> getPlayerCertificationInfos(Long playerId) {
        List<PlayerCertificationView> playerCertificationViews = playerCertificationReader.getPlayerCertificationInfos(
                playerId);
        return playerCertificationViews.stream()
                .map(PlayerCertificationResult.PlayerCertificationInfo::from)
                .toList();
    }

    @Transactional
    public PlayerCertificationResult.ChangedPlayerCertification changePlayerCertification(Long playerId, PlayerCertificationCommand.ChangePlayerCertification command) {
        PlayerCertification playerCertification = playerCertificationWriter.changePlayerCertification(
                playerId,
                command.certificationId(),
                command.acquiredDate(),
                command.expiresDate()
        );

        return PlayerCertificationResult.ChangedPlayerCertification.from(playerCertification);
    }

    @Transactional
    public PlayerCertificationResult.CreatedPlayerCertification createPlayerCertification(
            Long playerId,
            PlayerCertificationCommand.CreatePlayerCertification command
    ) {
        PlayerCertification playerCertification = playerCertificationWriter.createPlayerCertification(
                PlayerCertification.create(
                        playerId,
                        command.certificationId(),
                        command.acquiredDate(),
                        command.expiresDate()
                )
        );

        return PlayerCertificationResult.CreatedPlayerCertification.from(playerCertification);
    }

    @Transactional
    public void deletePlayerCertification(Long playerId, Long certificationId) {
        playerCertificationWriter.deletePlayerCertification(playerId, certificationId);
    }
}
