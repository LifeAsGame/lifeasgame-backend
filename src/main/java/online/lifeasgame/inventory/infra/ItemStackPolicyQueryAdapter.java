package online.lifeasgame.inventory.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.query.ItemStackPolicyQuery;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ItemStackPolicyQueryAdapter implements ItemStackPolicyQuery {

    private final JpaInventoryRepository jpaInventoryRepository;
    private final JpaMailboxRepository jpaMailboxRepository;

    @Override
    public long countInventoryStacksExceeding(Long itemId, int limit) {
        return jpaInventoryRepository.countStacksExceeding(itemId, limit);
    }

    @Override
    public long countMailboxStacksExceeding(Long itemId, int limit) {
        return jpaMailboxRepository.countStacksExceeding(itemId, limit);
    }
}
