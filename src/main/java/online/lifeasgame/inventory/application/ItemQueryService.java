package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemQueryService {

    private final ItemReader itemReader;

    public ItemResult.Detail getItem(Long id) {
        return ItemResult.Detail.from(itemReader.getByIdOrThrow(id));
    }

    public ItemResult.Page<ItemResult.Summary> search(
            String name,
            String category,
            String type,
            String rarity,
            Pageable pageable
    ) {
        Page<ItemResult.Summary> result = itemReader.search(
                name,
                ItemCategory.parseNullable(category),
                ItemType.parseNullable(type),
                Rarity.parseNullable(rarity),
                pageable
        ).map(ItemResult.Summary::from);
        return ItemResult.Page.from(result);
    }
}
