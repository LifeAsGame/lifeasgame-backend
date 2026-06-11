package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.PlayerCertification;

import java.util.Optional;

public interface PlayerCertificationRepository {
    PlayerCertification save(PlayerCertification playerCertification);

    Optional<PlayerCertification> findByPlayerIdAndCertificationId(Long playerId, Long certificationId);

    void deleteByPlayerIdAndCertificationId(Long playerId, Long certificationId);

    boolean existsByPlayerIdAndCertificationId(Long playerId, Long certificationId);
}
