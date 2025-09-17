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
public class PlayerCertificationWriter {

    private final PlayerCertificationRepository repository;

    public PlayerCertification grantCertification(PlayerCertification playerCertification) {
        return repository.save(playerCertification);
    }

    public PlayerCertification changePlayerCertification(
            Long playerId,
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        PlayerCertification playerCertification  = repository.findByPlayerIdAndCertificationId(playerId, certificationId)
                .orElseThrow(() -> new DomainException(PlayerCertificationError.PLAYER_CERTIFICATION_NOT_FOUND));

        playerCertification.changeDate(acquiredDate, expiresDate);

        return playerCertification;
    }
}
