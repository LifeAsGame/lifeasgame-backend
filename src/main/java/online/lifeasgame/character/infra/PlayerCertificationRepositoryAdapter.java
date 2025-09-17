package online.lifeasgame.character.infra;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerCertificationQuery;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.character.domain.repository.PlayerCertificationRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerCertificationRepositoryAdapter implements PlayerCertificationRepository, PlayerCertificationQuery {

    private final JpaPlayerCertificationRepository jpaRepository;

    @Override
    public PlayerCertification save(PlayerCertification playerCertification) {
        return jpaRepository.save(playerCertification);
    }

    @Override
    public Optional<PlayerCertification> findByPlayerIdAndCertificationId(Long playerId, Long certificationId) {
        return jpaRepository.findByPlayerIdAndCertificationId(playerId, certificationId);
    }

    @Override
    public void deleteByPlayerIdAndCertificationId(Long playerId, Long certificationId) {
        jpaRepository.deleteByPlayerIdAndCertificationId(playerId, certificationId);
    }

    @Override
    public boolean existsByPlayerIdAndCertificationId(Long playerId, Long certificationId) {
        return jpaRepository.existsByPlayerIdAndCertificationId(playerId, certificationId);
    }

    @Override
    public List<PlayerCertificationView> findPlayerCertificationInfos(Long playerId) {
        return jpaRepository.findPlayerCertificationViews(playerId);
    }
}
