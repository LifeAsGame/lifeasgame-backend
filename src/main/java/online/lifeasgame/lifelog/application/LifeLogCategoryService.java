package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.domain.*;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.repository.LifeLogCategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LifeLogCategoryService {
    private final LifeLogCategoryRepository categories;
    private final CollectionLogReader collections;
    private final ExerciseLogReader exercises;
    private final MediaLogReader media;
    private final CurrentPlayerAccessor players;

    public record Category(Long id, String kind, String source, String systemCode, String name) {
        static Category from(LifeLogCategory c) {
            return new Category(c.getId(), c.getKind().name(), c.getSource().name(), c.getSystemCode(),
                    c.getSource() == LifeLogCategory.Source.SYSTEM ? c.getSystemCode() : c.getName());
        }
    }

    @Transactional(readOnly = true)
    public List<String> systemCodes(String rawKind) {
        return switch (LifeLogCategoryKind.parse(rawKind)) {
            case COLLECTION -> Arrays.stream(CollectionCategory.values()).map(Enum::name).toList();
            case EXERCISE -> Arrays.stream(ExerciseCategory.values()).map(Enum::name).toList();
            case MEDIA -> Arrays.stream(MediaCategory.values()).map(Enum::name).toList();
        };
    }

    @Transactional(readOnly = true)
    public List<Category> mine(String rawKind) {
        return categories.findOwned(players.currentPlayerIdOrThrow(), LifeLogCategoryKind.parse(rawKind))
                .stream().filter(c -> !c.isHidden()).map(Category::from).toList();
    }

    @Transactional
    public Category addSystem(String rawKind, String rawCode) {
        Long owner = players.currentPlayerIdOrThrow();
        LifeLogCategoryKind kind = LifeLogCategoryKind.parse(rawKind);
        String code = kind.validateSystemCode(rawCode);
        categories.addSystem(owner, kind, code);
        return Category.from(categories.findSystem(owner, kind, code).orElseThrow());
    }

    @Transactional
    public void hideSystem(String rawKind, String rawCode) {
        Long owner = players.currentPlayerIdOrThrow();
        LifeLogCategoryKind kind = LifeLogCategoryKind.parse(rawKind);
        String code = kind.validateSystemCode(rawCode);
        LifeLogCategory category = categories.findSystem(owner, kind, code)
                .orElseThrow(() -> new DomainException(LifeLogError.CATEGORY_NOT_FOUND));
        category.hide();
    }

    @Transactional
    public Category createPersonal(String rawKind, String name) {
        Long owner = players.currentPlayerIdOrThrow();
        LifeLogCategory category = LifeLogCategory.personal(owner, LifeLogCategoryKind.parse(rawKind), name);
        if (categories.nameExists(owner, category.getKind(), category.getNormalizedName(), -1L)) {
            throw new DomainException(LifeLogError.CATEGORY_DUPLICATE);
        }
        try {
            return Category.from(categories.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(LifeLogError.CATEGORY_DUPLICATE);
        }
    }

    @Transactional
    public Category renamePersonal(Long id, String name) {
        Long owner = players.currentPlayerIdOrThrow();
        LifeLogCategory category = personal(id, owner, null);
        category.rename(name);
        if (categories.nameExists(owner, category.getKind(), category.getNormalizedName(), id)) {
            throw new DomainException(LifeLogError.CATEGORY_DUPLICATE);
        }
        try {
            return Category.from(categories.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(LifeLogError.CATEGORY_DUPLICATE);
        }
    }

    @Transactional
    public void deletePersonal(Long id) {
        categories.delete(personal(id, players.currentPlayerIdOrThrow(), null));
    }

    @Transactional
    public Long assign(String rawKind, Long recordId, Long categoryId) {
        Long owner = players.currentPlayerIdOrThrow();
        LifeLogCategoryKind kind = LifeLogCategoryKind.parse(rawKind);
        Long selected = categoryId == null ? null : personal(categoryId, owner, kind).getId();
        switch (kind) {
            case COLLECTION -> collections.getByIdAndPlayerIdOrThrow(recordId, owner).assignPersonalCategory(selected);
            case EXERCISE -> exercises.getByIdAndPlayerIdOrThrow(recordId, owner).assignPersonalCategory(selected);
            case MEDIA -> media.getByPlayerIdAndIdOrThrow(owner, recordId).assignPersonalCategory(selected);
        }
        return selected;
    }

    // Called inside the existing record-creation transaction, before the record is persisted.
    public Long requirePersonal(Long id, Long owner, LifeLogCategoryKind kind) {
        return id == null ? null : personal(id, owner, kind).getId();
    }

    @Transactional(readOnly = true)
    public void validateFilter(Long owner, LifeLogCategoryKind kind, Long id, boolean unclassified) {
        if (id != null && unclassified) throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        if (id == null) return;
        LifeLogCategory category = categories.findOwnedById(id, owner)
                .orElseThrow(() -> new DomainException(LifeLogError.CATEGORY_NOT_FOUND));
        if (category.getSource() != LifeLogCategory.Source.PERSONAL || category.getKind() != kind) {
            throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        }
    }

    @Transactional(readOnly = true)
    public LifeLogCategoryKind filterKind(Long owner, Long id) {
        LifeLogCategory category = categories.findOwnedById(id, owner)
                .orElseThrow(() -> new DomainException(LifeLogError.CATEGORY_NOT_FOUND));
        if (category.getSource() != LifeLogCategory.Source.PERSONAL) {
            throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        }
        return category.getKind();
    }

    private LifeLogCategory personal(Long id, Long owner, LifeLogCategoryKind kind) {
        LifeLogCategory category = categories.lockOwned(id, owner)
                .orElseThrow(() -> new DomainException(LifeLogError.CATEGORY_NOT_FOUND));
        if (category.getSource() != LifeLogCategory.Source.PERSONAL) {
            throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        }
        if (kind != null && category.getKind() != kind) {
            throw new DomainException(LifeLogError.INVALID_CATEGORY_KIND);
        }
        return category;
    }
}
