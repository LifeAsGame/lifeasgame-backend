package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.repository.ItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class ItemWriter {

    private final ItemRepository repository;

    public Item create(Item item) {
        return repository.save(item);
    }

    public void delete(Item item) {
        repository.delete(item);
    }
}
