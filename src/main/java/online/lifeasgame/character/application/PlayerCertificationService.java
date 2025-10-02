package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerCertificationService {

    private final PlayerCertificationWriter playerCertificationWriter;
    private final PlayerCertificationReader playerCertificationReader;

    private final CertificationReader certificationReader;
    private final PlayerReader playerReader;

    @Transactional
    public PlayerCertificationResult.GrantedCertification grantCertification(PlayerCertificationCommand.GrantCertification command) {
        if (playerReader.notExists(command.playerId())) {
            throw new DomainException(PlayerError.PLAYER_NOT_FOUND);
        }

        Certification certification = certificationReader.getCertification(command.certificationId());

        PlayerCertification saved = playerCertificationWriter.grantCertification(
                PlayerCertification.create(
                        command.playerId(),
                        command.certificationId(),
                        command.acquiredDate(),
                        command.expiresDate()
                )
        );

        return PlayerCertificationResult.GrantedCertification.of(
                saved.getPlayerId(),
                saved.getCertificationId(),
                certification.getName(),
                certification.getIssuer(),
                certification.getCategory().name(),
                saved.getAcquiredDate(),
                saved.getExpiresDate(),
                saved.getGrantedAt()
        );
    }

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
