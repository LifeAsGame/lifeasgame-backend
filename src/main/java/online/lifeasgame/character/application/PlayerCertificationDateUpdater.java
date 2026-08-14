package online.lifeasgame.character.application;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.character.domain.error.PlayerCertificationError;
import online.lifeasgame.character.domain.repository.PlayerCertificationRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerCertificationDateUpdater {

    private final PlayerCertificationRepository repository;

    public PlayerCertification update(
            Long playerId,
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        PlayerCertification playerCertification = repository.findByPlayerIdAndCertificationId(playerId, certificationId)
                .orElseThrow(() -> new DomainException(PlayerCertificationError.PLAYER_CERTIFICATION_NOT_FOUND));

        LocalDate effectiveAcquiredDate = acquiredDate != null ? acquiredDate : playerCertification.getAcquiredDate();
        LocalDate effectiveExpiresDate = expiresDate != null ? expiresDate : playerCertification.getExpiresDate();
        playerCertification.changeDates(effectiveAcquiredDate, effectiveExpiresDate);

        return playerCertification;
    }
}
