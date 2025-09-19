package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AdminPlayerCertificationCommand;
import online.lifeasgame.character.application.result.AdminPlayerCertificationResult;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPlayerCertificationService {

    private final CertificationReader certificationReader;
    private final PlayerCertificationWriter playerCertificationWriter;
    private final PlayerReader playerReader;

    @Transactional
    public AdminPlayerCertificationResult.GrantedCertification grantCertification(AdminPlayerCertificationCommand.GrantCertification command) {
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

        return AdminPlayerCertificationResult.GrantedCertification.of(
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
}
