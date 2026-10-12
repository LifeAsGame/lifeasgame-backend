package online.lifeasgame.character.domain.repository;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.domain.PersonalCategory;
import online.lifeasgame.character.domain.PersonalCategory.Kind;

public interface PersonalCategoryRepository {
    List<PersonalCategory> findByOwnerPlayerIdAndKind(Long ownerPlayerId, Kind kind);
    Optional<PersonalCategory> findLocked(Long id, Long ownerPlayerId, Kind kind);
    boolean existsByIdAndOwnerPlayerIdAndKind(Long id, Long ownerPlayerId, Kind kind);
    boolean existsByOwnerPlayerIdAndKindAndNormalizedName(Long ownerPlayerId, Kind kind, String normalizedName);
    PersonalCategory saveAndFlush(PersonalCategory category);
    void delete(PersonalCategory category);
}
