package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.character.domain.error.PlayerCertificationError;
import online.lifeasgame.character.domain.repository.PlayerCertificationRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerCertificationWriter {

    private final PlayerCertificationRepository repository;

    public PlayerCertification create(PlayerCertification playerCertification) {
        return repository.save(playerCertification);
    }

    public PlayerCertification changeDates(
            Long playerId,
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        PlayerCertification playerCertification = repository.findByPlayerIdAndCertificationId(playerId, certificationId)
                .orElseThrow(() -> new DomainException(PlayerCertificationError.PLAYER_CERTIFICATION_NOT_FOUND));

        playerCertification.changeDate(acquiredDate, expiresDate);

        return playerCertification;
    }

    public void deletePlayerCertification(Long playerId, Long certificationId) {
        if (!repository.existsByPlayerIdAndCertificationId(playerId, certificationId)) {
            throw new DomainException(PlayerCertificationError.PLAYER_CERTIFICATION_NOT_FOUND);
        }

        repository.deleteByPlayerIdAndCertificationId(playerId, certificationId);
    }
}
