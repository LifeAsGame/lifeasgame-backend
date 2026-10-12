package online.lifeasgame.lifelog.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.LifeLogCategory;
import online.lifeasgame.lifelog.domain.LifeLogCategoryKind;
import online.lifeasgame.lifelog.domain.repository.LifeLogCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LifeLogCategoryRepositoryAdapter implements LifeLogCategoryRepository {
    private final LifeLogCategoryJpaRepository jpa;

    public List<LifeLogCategory> findOwned(Long owner, LifeLogCategoryKind kind) {
        return jpa.findByOwnerPlayerIdAndKindOrderById(owner, kind);
    }
    public Optional<LifeLogCategory> findSystem(Long owner, LifeLogCategoryKind kind, String code) {
        return jpa.findByOwnerPlayerIdAndKindAndSystemCode(owner, kind, code);
    }
    public Optional<LifeLogCategory> lockOwned(Long id, Long owner) { return jpa.lockOwned(id, owner); }
    public Optional<LifeLogCategory> findOwnedById(Long id, Long owner) { return jpa.findByIdAndOwnerPlayerId(id, owner); }
    public boolean nameExists(Long owner, LifeLogCategoryKind kind, String normalizedName, Long exceptId) {
        return jpa.existsByOwnerPlayerIdAndKindAndNormalizedNameAndIdNot(owner, kind, normalizedName, exceptId);
    }
    public LifeLogCategory saveAndFlush(LifeLogCategory category) { return jpa.saveAndFlush(category); }
    public void addSystem(Long owner, LifeLogCategoryKind kind, String code) { jpa.addSystem(owner, kind.name(), code); }
    public void delete(LifeLogCategory category) { jpa.delete(category); jpa.flush(); }
}
