package online.lifeasgame.character.application;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerCertificationQuery;
import online.lifeasgame.character.application.query.PlayerHobbyQuery;
import online.lifeasgame.character.application.query.CharacterCatalogQuery;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.domain.CertificationCategory;
import online.lifeasgame.character.domain.HobbyCategory;
import online.lifeasgame.character.domain.PersonalCategory;
import online.lifeasgame.character.domain.PersonalCategory.Kind;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.error.PersonalCategoryError;
import online.lifeasgame.character.domain.repository.PersonalCategoryRepository;
import online.lifeasgame.character.domain.repository.PlayerCertificationRepository;
import online.lifeasgame.character.domain.repository.PlayerHobbyRepository;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalCategoryService {
    public enum Source { SYSTEM, OFFICIAL, PERSONAL }
    private final PersonalCategoryRepository categories;
    private final PlayerCertificationRepository certifications;
    private final PlayerHobbyRepository hobbies;
    private final PlayerCertificationQuery certificationQuery;
    private final PlayerHobbyQuery hobbyQuery;
    private final CurrentPlayerAccessor currentPlayer;
    private final CharacterCatalogQuery catalog;

    public record Category(Long id, String code, String name, Source source, Kind kind) {
        static Category personal(PersonalCategory category) {
            return new Category(category.getId(), null, category.getName(), Source.PERSONAL, category.getKind());
        }
        static Category system(String code, Kind kind) {
            return new Category(null, code, code, Source.SYSTEM, kind);
        }
    }

    public record Assignment(Long itemId, Long personalCategoryId) {}

    @Transactional(readOnly = true)
    public List<Category> list(Kind kind) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        List<Category> systems = kind == Kind.CERTIFICATION
                ? Arrays.stream(CertificationCategory.values()).map(c -> Category.system(c.name(), kind)).toList()
                : Arrays.stream(HobbyCategory.values()).map(c -> Category.system(c.name(), kind)).toList();
        List<Category> result = new java.util.ArrayList<>(systems);
        categories.findByOwnerPlayerIdAndKind(owner, kind).stream().map(Category::personal).forEach(result::add);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Category> owned(Kind kind) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        List<Category> result = new java.util.ArrayList<>();
        if (kind == Kind.CERTIFICATION) {
            catalog.ownedLegacyCertificationCategories(owner).stream()
                    .map(category -> Category.system(category.name(), kind)).forEach(result::add);
            catalog.ownedOfficialGroups(owner).stream()
                    .map(group -> new Category(null, "HRDK:" + group.majorCode() + ":" + group.minorCode(),
                            group.majorName() + " / " + group.minorName(), Source.OFFICIAL, kind))
                    .forEach(result::add);
        } else {
            catalog.ownedHobbyCategories(owner).stream()
                    .map(category -> Category.system(category.name(), kind)).forEach(result::add);
        }
        categories.findByOwnerPlayerIdAndKind(owner, kind).stream().map(Category::personal).forEach(result::add);
        return result;
    }

    @Transactional
    public Category create(Kind kind, String name) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        PersonalCategory category = new PersonalCategory(owner, kind, name);
        if (categories.existsByOwnerPlayerIdAndKindAndNormalizedName(owner, kind, category.getNormalizedName())) {
            throw new DomainException(PersonalCategoryError.DUPLICATE_NAME);
        }
        try {
            return Category.personal(categories.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(PersonalCategoryError.DUPLICATE_NAME);
        }
    }

    @Transactional
    public Category rename(Kind kind, Long id, String name) {
        PersonalCategory category = ownedLocked(kind, id);
        category.rename(name);
        try {
            return Category.personal(categories.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(PersonalCategoryError.DUPLICATE_NAME);
        }
    }

    @Transactional
    public void delete(Kind kind, Long id) {
        categories.delete(ownedLocked(kind, id));
    }

    @Transactional
    public Assignment assign(Kind kind, Long itemId, Long categoryId) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        if (categoryId != null) ownedLocked(kind, categoryId);
        if (kind == Kind.CERTIFICATION) {
            PlayerCertification item = certifications.findByPlayerIdAndCertificationId(owner, itemId)
                    .orElseThrow(() -> new DomainException(PersonalCategoryError.OWNED_ITEM_NOT_FOUND));
            item.assignPersonalCategory(categoryId);
        } else {
            PlayerHobby item = hobbies.findByPlayerIdAndHobbyId(owner, itemId)
                    .orElseThrow(() -> new DomainException(PersonalCategoryError.OWNED_ITEM_NOT_FOUND));
            item.assignPersonalCategory(categoryId);
        }
        return new Assignment(itemId, categoryId);
    }

    @Transactional(readOnly = true)
    public List<PlayerCertificationResult.Info> certificationItems(Long id, String systemCode) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        if (id != null) requireOwned(Kind.CERTIFICATION, id, owner);
        CertificationCategory system = systemCode == null ? null : CertificationCategory.parse(systemCode);
        return certificationQuery.findViewsByPlayerId(owner).stream()
                .filter(v -> id != null ? id.equals(v.getPersonalCategoryId()) : system == v.getCategory())
                .map(PlayerCertificationResult.Info::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerHobbyResult.Info> hobbyItems(Long id, String systemCode) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        if (id != null) requireOwned(Kind.HOBBY, id, owner);
        HobbyCategory system = systemCode == null ? null : HobbyCategory.parse(systemCode);
        return hobbyQuery.findViewsByPlayerId(owner).stream()
                .filter(v -> id != null ? id.equals(v.getPersonalCategoryId()) : system == v.getCategory())
                .map(PlayerHobbyResult.Info::from).toList();
    }

    private PersonalCategory ownedLocked(Kind kind, Long id) {
        return categories.findLocked(id, currentPlayer.currentPlayerIdOrThrow(), kind)
                .orElseThrow(() -> new DomainException(PersonalCategoryError.NOT_FOUND));
    }

    private void requireOwned(Kind kind, Long id, Long owner) {
        if (!categories.existsByIdAndOwnerPlayerIdAndKind(id, owner, kind)) {
            throw new DomainException(PersonalCategoryError.NOT_FOUND);
        }
    }
}
