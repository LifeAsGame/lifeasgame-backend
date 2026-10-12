package online.lifeasgame.lifelog.domain.repository;

import online.lifeasgame.lifelog.domain.LifeLogCategory;
import online.lifeasgame.lifelog.domain.LifeLogCategoryKind;

import java.util.List;
import java.util.Optional;

public interface LifeLogCategoryRepository {
    List<LifeLogCategory> findOwned(Long owner, LifeLogCategoryKind kind);
    Optional<LifeLogCategory> findSystem(Long owner, LifeLogCategoryKind kind, String code);
    Optional<LifeLogCategory> lockOwned(Long id, Long owner);
    Optional<LifeLogCategory> findOwnedById(Long id, Long owner);
    boolean nameExists(Long owner, LifeLogCategoryKind kind, String normalizedName, Long exceptId);
    LifeLogCategory saveAndFlush(LifeLogCategory category);
    void addSystem(Long owner, LifeLogCategoryKind kind, String code);
    void delete(LifeLogCategory category);
}
