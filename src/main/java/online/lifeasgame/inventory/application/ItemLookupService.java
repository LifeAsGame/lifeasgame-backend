package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.ItemLookupApi;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCode;
import online.lifeasgame.inventory.domain.error.ItemError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ItemLookupService implements ItemLookupApi {

    private final ItemReader itemReader;

    @Override
    public ItemReference getByCode(String code) {
        Item item = itemReader.getByCodeOrThrow(ItemCode.of(code));
        return reference(item);
    }

    @Override
    public ItemReference getById(Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new DomainException(ItemError.ITEM_ID_INVALID);
        }
        return reference(itemReader.getByIdOrThrow(itemId));
    }

    private static ItemReference reference(Item item) {
        if (item.getCode() == null
                || item.getCode().value() == null
                || item.getCode().value().isBlank()) {
            throw new DomainException(ItemError.ITEM_CODE_NOT_FOUND);
        }
        return new ItemReference(item.getId(), item.getCode().value().strip());
    }
}
