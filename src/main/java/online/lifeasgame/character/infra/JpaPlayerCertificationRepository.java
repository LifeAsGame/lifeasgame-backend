package online.lifeasgame.character.infra;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.domain.PlayerCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaPlayerCertificationRepository extends JpaRepository<PlayerCertification, Long> {

    @Query(
            """
                SELECT t.id AS certificationId,
                       t.name AS name,
                       t.issuer AS issuer,
                       t.category  AS category,
                       pc.acquiredDate AS acquiredDate,
                       pc.expiresDate AS expiresDate,
                       pc.grantedAt AS grantedAt
                FROM PlayerCertification pc
                JOIN Certification t ON t.id = pc.certificationId
                WHERE pc.playerId = :playerId
                ORDER BY pc.acquiredDate DESC
        """)
    List<PlayerCertificationView> findPlayerCertificationViews(Long playerId);

    Optional<PlayerCertification> findByPlayerIdAndCertificationId(Long playerId, Long certificationId);
}
