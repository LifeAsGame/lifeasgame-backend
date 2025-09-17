package online.lifeasgame.character.domain.repository;

import java.util.Optional;
import online.lifeasgame.character.domain.PlayerCertification;

public interface PlayerCertificationRepository {
    PlayerCertification save(PlayerCertification playerCertification);

    Optional<PlayerCertification> findByPlayerIdAndCertificationId(Long playerId, Long certificationId);
}
