package online.lifeasgame.character.infra;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PersonalCategory;
import online.lifeasgame.character.domain.PersonalCategory.Kind;
import online.lifeasgame.character.domain.repository.PersonalCategoryRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PersonalCategoryRepositoryAdapter implements PersonalCategoryRepository {
    private final JpaPersonalCategoryRepository jpa;

    public List<PersonalCategory> findByOwnerPlayerIdAndKind(Long owner, Kind kind) {
        return jpa.findByOwnerPlayerIdAndKindOrderById(owner, kind);
    }
    public Optional<PersonalCategory> findLocked(Long id, Long owner, Kind kind) {
        return jpa.findLocked(id, owner, kind);
    }
    public boolean existsByOwnerPlayerIdAndKindAndNormalizedName(Long owner, Kind kind, String name) {
        return jpa.existsByOwnerPlayerIdAndKindAndNormalizedName(owner, kind, name);
    }
    public boolean existsByIdAndOwnerPlayerIdAndKind(Long id, Long owner, Kind kind) {
        return jpa.existsByIdAndOwnerPlayerIdAndKind(id, owner, kind);
    }
    public PersonalCategory saveAndFlush(PersonalCategory category) { return jpa.saveAndFlush(category); }
    public void delete(PersonalCategory category) { jpa.delete(category); }
}
