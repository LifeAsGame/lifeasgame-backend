package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryRewardDeliveryApi;
import online.lifeasgame.inventory.domain.InstanceAttrs;
import online.lifeasgame.inventory.domain.InventoryRewardDelivery;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCarryPolicy;
import online.lifeasgame.inventory.domain.ItemCode;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.repository.InventoryRewardDeliveryRepository;
import online.lifeasgame.inventory.domain.repository.PlayerMailboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InventoryRewardDeliveryService implements InventoryRewardDeliveryApi {

    private final InventoryContainerProvisioningService provisioningService;
    private final PlayerMailboxRepository mailboxRepository;
    private final InventoryRewardDeliveryRepository deliveryRepository;
    private final ItemReader itemReader;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public RewardDeliveryResult deliverReward(
            Long rewardLineId,
            Long playerId,
            String itemCode,
            long quantity
    ) {
        validateRewardLineId(rewardLineId);
        validatePlayerId(playerId);
        ItemCode normalizedItemCode = normalizeItemCode(itemCode);
        int mailboxQuantity = validateQuantity(quantity);

        provisioningService.ensureContainers(playerId);
        PlayerMailbox mailbox = mailboxRepository
                .findByPlayerIdForUpdate(playerId)
                .orElseThrow(() -> new DomainException(
                        InventoryError.CONTAINER_NOT_FOUND
                ));

        var existing = deliveryRepository.findByRewardLineId(rewardLineId);
        if (existing.isPresent()) {
            InventoryRewardDelivery delivery = existing.get();
            delivery.assertMatches(
                    rewardLineId,
                    playerId,
                    normalizedItemCode,
                    quantity
            );
            return result(delivery, true);
        }

        Item item = itemReader.getByCodeOrThrow(normalizedItemCode);
        if (item.getCode() == null
                || !item.getCode().value().equals(normalizedItemCode.value())) {
            throw new DomainException(InventoryError.REWARD_ITEM_CODE_INVALID);
        }

        mailbox.deliver(
                ItemCarryPolicy.from(item),
                mailboxQuantity,
                InstanceAttrs.empty(),
                true
        );

        InventoryRewardDelivery saved = deliveryRepository.save(
                InventoryRewardDelivery.create(
                        rewardLineId,
                        playerId,
                        normalizedItemCode,
                        item.getId(),
                        quantity,
                        Instant.now(clock)
                )
        );
        return result(saved, false);
    }

    private static RewardDeliveryResult result(
            InventoryRewardDelivery delivery,
            boolean replayed
    ) {
        return new RewardDeliveryResult(
                delivery.getId(),
                delivery.getRewardLineId(),
                delivery.getPlayerId(),
                delivery.getItemId(),
                delivery.getItemCode(),
                delivery.getQuantity(),
                replayed
        );
    }

    private static void validateRewardLineId(Long rewardLineId) {
        if (rewardLineId == null || rewardLineId <= 0) {
            throw new DomainException(InventoryError.REWARD_LINE_ID_INVALID);
        }
    }

    private static void validatePlayerId(Long playerId) {
        if (playerId == null || playerId <= 0) {
            throw new DomainException(InventoryError.PLAYER_ID_INVALID);
        }
    }

    private static ItemCode normalizeItemCode(String itemCode) {
        if (itemCode == null) {
            throw new DomainException(InventoryError.REWARD_ITEM_CODE_INVALID);
        }
        String normalized = itemCode.strip();
        if (normalized.isEmpty() || normalized.length() > ItemCode.MAX_LENGTH) {
            throw new DomainException(InventoryError.REWARD_ITEM_CODE_INVALID);
        }
        return ItemCode.of(normalized);
    }

    private static int validateQuantity(long quantity) {
        if (quantity <= 0 || quantity > Integer.MAX_VALUE) {
            throw new DomainException(InventoryError.REWARD_QUANTITY_INVALID);
        }
        return (int) quantity;
    }
}
