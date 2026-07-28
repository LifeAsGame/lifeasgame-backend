package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.internal.ItemLookupApi;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCode;
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
        return new ItemReference(item.getId(), item.getCode().value());
    }
}
