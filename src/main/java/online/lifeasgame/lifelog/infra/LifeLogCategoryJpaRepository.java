package online.lifeasgame.lifelog.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.lifelog.domain.LifeLogCategory;
import online.lifeasgame.lifelog.domain.LifeLogCategoryKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LifeLogCategoryJpaRepository extends JpaRepository<LifeLogCategory, Long> {
    List<LifeLogCategory> findByOwnerPlayerIdAndKindOrderById(Long ownerPlayerId, LifeLogCategoryKind kind);
    Optional<LifeLogCategory> findByOwnerPlayerIdAndKindAndSystemCode(Long owner, LifeLogCategoryKind kind, String code);
    Optional<LifeLogCategory> findByIdAndOwnerPlayerId(Long id, Long owner);
    boolean existsByOwnerPlayerIdAndKindAndNormalizedNameAndIdNot(Long owner, LifeLogCategoryKind kind, String normalizedName, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LifeLogCategory c where c.id = :id and c.ownerPlayerId = :owner")
    Optional<LifeLogCategory> lockOwned(@Param("id") Long id, @Param("owner") Long owner);

    @Modifying
    @Query(value = """
        INSERT INTO lifelog_categories (owner_player_id, kind, source, system_code, hidden)
        VALUES (:owner, :kind, 'SYSTEM', :code, b'0')
        ON DUPLICATE KEY UPDATE hidden = b'0'
        """, nativeQuery = true)
    void addSystem(@Param("owner") Long owner, @Param("kind") String kind, @Param("code") String code);
}
