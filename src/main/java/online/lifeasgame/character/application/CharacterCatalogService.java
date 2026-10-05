package online.lifeasgame.character.application;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.CharacterCatalogQuery;
import online.lifeasgame.character.application.query.CharacterCatalogQuery.Group;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import online.lifeasgame.character.domain.PersonalCategory.Kind;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterCatalogService {
    private final CharacterCatalogQuery catalog;
    private final CurrentPlayerAccessor currentPlayer;

    public record Item(Long ownedItemId, Long catalogItemId, String name, String category, String source, String sourceCode,
                       String majorCode, String majorName, String minorCode, String minorName, String issuer,
                       String administeringAgency, String detail, String detailStatus, String sourceUrl,
                       Instant fetchedAt, boolean owned) {}
    public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}
    public record Category(String majorCode, String majorName, String minorCode, String minorName, String source) {}

    public PageResult<Item> search(Kind kind, String query, String majorCode, String minorCode, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page or size");
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        String searchTerm = query == null || query.isBlank() ? null : query.strip();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id")));
        if (kind == Kind.HOBBY && minorCode != null) throw new IllegalArgumentException("Hobbies have no minor category");
        Page<Item> result = kind == Kind.CERTIFICATION
                ? catalog.certifications(searchTerm, majorCode, minorCode, pageable)
                    .map(item -> certification(item, owner))
                : catalog.hobbies(searchTerm, majorCode == null ? null : HobbyCategory.parse(majorCode), pageable)
                    .map(item -> hobby(item, owner));
        return new PageResult<>(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    public Item get(Kind kind, Long id) {
        Long owner = currentPlayer.currentPlayerIdOrThrow();
        return kind == Kind.CERTIFICATION ? certification(catalog.certification(id), owner) : hobby(catalog.hobby(id), owner);
    }

    public List<Category> categories(Kind kind) {
        if (kind == Kind.HOBBY) {
            return Arrays.stream(HobbyCategory.values())
                    .map(value -> new Category(value.name(), value.name(), null, null, "SERVICE"))
                    .toList();
        }
        List<Category> result = new java.util.ArrayList<>(catalog.officialGroups().stream()
                .map(group -> new Category(group.majorCode(), group.majorName(), group.minorCode(), group.minorName(), "HRDK"))
                .toList());
        result.add(new Category("LEGACY", "Legacy/Other", null, null, "LEGACY"));
        return result;
    }

    private Item certification(Certification item, Long owner) {
        Long ownedItemId = catalog.ownedCertificationId(owner, item.getId());
        return new Item(ownedItemId, item.getId(), item.getName(), item.getCategory().name(),
                item.getProvider() == null ? "LEGACY" : item.getProvider(), item.getSourceCode(),
                item.getMajorCode(), item.getMajorName(), item.getMinorCode(), item.getMinorName(),
                item.getIssuer(), item.getAdministeringAgency(), item.getDetail(), item.getDetailStatus(),
                item.getSourceUrl(), item.getFetchedAt(), ownedItemId != null);
    }

    private Item hobby(Hobby item, Long owner) {
        Long ownedItemId = catalog.ownedHobbyId(owner, item.getId());
        return new Item(ownedItemId, item.getId(), item.getName(), item.getCategory().name(), item.getSource(), null,
                item.getCategory().name(), item.getCategory().name(), null, null, null, null, null, null,
                null, null, ownedItemId != null);
    }
}
