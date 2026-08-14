package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.error.PlayerCertificationError;
import online.lifeasgame.character.domain.repository.PlayerCertificationRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerCertificationRevoker {

    private final PlayerCertificationRepository repository;

    public void revoke(Long playerId, Long certificationId) {
        if (!repository.existsByPlayerIdAndCertificationId(playerId, certificationId)) {
            throw new DomainException(PlayerCertificationError.PLAYER_CERTIFICATION_NOT_FOUND);
        }

        repository.deleteByPlayerIdAndCertificationId(playerId, certificationId);
    }
}
