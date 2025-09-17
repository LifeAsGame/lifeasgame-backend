package online.lifeasgame.character.infra;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.domain.PlayerCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPlayerCertificationRepository extends JpaRepository<PlayerCertification, Long> {

    @Query(
        """
            SELECT c.id AS certificationId,
                   c.name AS name,
                   c.issuer AS issuer,
                   c.category  AS category,
                   pc.acquiredDate AS acquiredDate,
                   pc.expiresDate AS expiresDate,
                   pc.grantedAt AS grantedAt
            FROM PlayerCertification pc
            JOIN Certification c ON c.id = pc.certificationId
            WHERE pc.playerId = :playerId
            ORDER BY pc.acquiredDate DESC
        """
    )
    List<PlayerCertificationView> findPlayerCertificationViews(@Param("playerId") Long playerId);

    Optional<PlayerCertification> findByPlayerIdAndCertificationId(Long playerId, Long certificationId);

    void deleteByPlayerIdAndCertificationId(Long playerId, Long certificationId);

    boolean existsByPlayerIdAndCertificationId(Long playerId, Long certificationId);
}
