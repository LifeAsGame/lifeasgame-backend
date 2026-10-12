package online.lifeasgame.character.infra;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.domain.PersonalCategory;
import online.lifeasgame.character.domain.PersonalCategory.Kind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPersonalCategoryRepository extends JpaRepository<PersonalCategory, Long> {
    List<PersonalCategory> findByOwnerPlayerIdAndKindOrderById(Long ownerPlayerId, Kind kind);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PersonalCategory c where c.id = :id and c.ownerPlayerId = :owner and c.kind = :kind")
    Optional<PersonalCategory> findLocked(@Param("id") Long id, @Param("owner") Long owner, @Param("kind") Kind kind);

    boolean existsByOwnerPlayerIdAndKindAndNormalizedName(Long ownerPlayerId, Kind kind, String normalizedName);
    boolean existsByIdAndOwnerPlayerIdAndKind(Long id, Long ownerPlayerId, Kind kind);
}
