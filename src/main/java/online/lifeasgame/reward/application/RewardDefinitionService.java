package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.ItemLookupApi;
import online.lifeasgame.reward.application.command.RewardDefinitionCommand;
import online.lifeasgame.reward.application.result.RewardDefinitionResult;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.RewardType;
import online.lifeasgame.reward.domain.error.RewardError;
import online.lifeasgame.reward.domain.repository.RewardDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RewardDefinitionService {

    private final RewardDefinitionReader definitionReader;
    private final RewardDefinitionRepository definitionRepository;
    private final ItemLookupApi itemLookupApi;

    @Transactional
    public RewardDefinitionResult.Detail create(
            RewardDefinitionCommand.Create command
    ) {
        String itemCode = resolveItemCode(
                command.rewardType(), command.itemId()
        );
        RewardDefinition definition = RewardDefinition.create(
                command.code(),
                command.name(),
                command.rewardType(),
                command.amount(),
                command.itemId(),
                itemCode,
                command.active()
        );
        return RewardDefinitionResult.Detail.from(
                definitionRepository.save(definition)
        );
    }

    @Transactional
    public RewardDefinitionResult.Detail update(
            Long definitionId,
            RewardDefinitionCommand.Update command
    ) {
        RewardDefinition definition = definitionReader.getByIdOrThrow(
                definitionId
        );
        String itemCode = resolveItemCode(
                command.rewardType(), command.itemId()
        );
        definition.update(
                command.code(),
                command.name(),
                command.rewardType(),
                command.amount(),
                command.itemId(),
                itemCode,
                command.active()
        );
        return RewardDefinitionResult.Detail.from(
                definitionRepository.save(definition)
        );
    }

    private String resolveItemCode(RewardType rewardType, Long itemId) {
        if (rewardType != RewardType.ITEM) {
            return null;
        }
        if (itemId == null) {
            throw new DomainException(RewardError.REWARD_ITEM_ID_REQUIRED);
        }
        if (itemId <= 0) {
            throw new DomainException(
                    RewardError.REWARD_ITEM_ID_MUST_BE_POSITIVE
            );
        }
        ItemLookupApi.ItemReference reference = itemLookupApi.getById(itemId);
        if (!Objects.equals(itemId, reference.id())) {
            throw new DomainException(
                    RewardError.REWARD_ITEM_REFERENCE_INCONSISTENT
            );
        }
        if (reference.code() == null || reference.code().isBlank()) {
            throw new DomainException(RewardError.REWARD_ITEM_CODE_REQUIRED);
        }
        String itemCode = reference.code().strip();
        if (itemCode.length() > 80) {
            throw new DomainException(RewardError.REWARD_ITEM_CODE_TOO_LONG);
        }
        return itemCode;
    }
}
