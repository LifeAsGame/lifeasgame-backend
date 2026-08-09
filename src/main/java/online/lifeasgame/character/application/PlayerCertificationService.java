package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
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
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public PlayerCertificationResult.Created createCertification(PlayerCertificationCommand.Create command) {
        return createCertification(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public PlayerCertificationResult.Created createCertification(Long playerId, PlayerCertificationCommand.Create command) {
        playerReader.assertExistsById(playerId);

        Certification certification = certificationReader.getByIdOrThrow(command.certificationId());

        PlayerCertification playerCertification = playerCertificationWriter.create(
                PlayerCertification.create(
                        playerId,
                        command.certificationId(),
                        command.acquiredDate(),
                        command.expiresDate()
                )
        );

        return PlayerCertificationResult.Created.from(playerCertification, certification);
    }

    @Transactional(readOnly = true)
    public List<PlayerCertificationResult.Info> getPlayerCertificationInfos() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<PlayerCertificationView> playerCertificationViews = playerCertificationReader.getViewByPlayerId(playerId);
        return playerCertificationViews.stream()
                .map(PlayerCertificationResult.Info::from)
                .toList();
    }

    @Transactional
    public PlayerCertificationResult.Changed changePlayerCertification(PlayerCertificationCommand.Change command) {
        return changePlayerCertification(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public PlayerCertificationResult.Changed changePlayerCertification(Long playerId, PlayerCertificationCommand.Change command) {
        PlayerCertification playerCertification = playerCertificationWriter.changeDates(
                playerId,
                command.certificationId(),
                command.acquiredDate(),
                command.expiresDate()
        );

        return PlayerCertificationResult.Changed.from(playerCertification);
    }

    @Transactional
    public void deletePlayerCertification(Long certificationId) {
        deletePlayerCertification(currentPlayerAccessor.currentPlayerIdOrThrow(), certificationId);
    }

    @Transactional
    public void deletePlayerCertification(Long playerId, Long certificationId) {
        playerCertificationWriter.deletePlayerCertification(playerId, certificationId);
    }

    @Transactional
    public PlayerCertificationResult.Revoked revokeCertification(Long playerId, Long certificationId) {
        playerCertificationWriter.deletePlayerCertification(playerId, certificationId);
        return new PlayerCertificationResult.Revoked(playerId, certificationId);
    }
}
