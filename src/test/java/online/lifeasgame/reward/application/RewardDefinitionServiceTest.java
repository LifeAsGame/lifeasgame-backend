package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.ItemLookupApi;
import online.lifeasgame.reward.application.command.RewardDefinitionCommand;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.RewardType;
import online.lifeasgame.reward.domain.error.RewardError;
import online.lifeasgame.reward.domain.repository.RewardDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardDefinitionService")
class RewardDefinitionServiceTest {

    @Mock
    private RewardDefinitionReader definitionReader;

    @Mock
    private RewardDefinitionRepository definitionRepository;

    @Mock
    private ItemLookupApi itemLookupApi;

    private RewardDefinitionService service;

    @BeforeEach
    void setUp() {
        service = new RewardDefinitionService(
                definitionReader,
                definitionRepository,
                itemLookupApi
        );
        org.mockito.Mockito.lenient()
                .when(definitionRepository.save(
                        org.mockito.ArgumentMatchers.any()
                ))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("ITEM 생성은 getById로 itemId와 stable code를 함께 확정한다")
    void createsItemWithResolvedSnapshot() {
        given(itemLookupApi.getById(77L)).willReturn(
                new ItemLookupApi.ItemReference(77L, "  IT_STABLE  ")
        );

        var result = service.create(new RewardDefinitionCommand.Create(
                "RD_ITEM", "Item", RewardType.ITEM, 2L, 77L, true
        ));

        assertThat(result.itemId()).isEqualTo(77L);
        assertThat(result.itemCode()).isEqualTo("IT_STABLE");
        verify(itemLookupApi).getById(77L);
    }

    @Test
    @DisplayName("EXP 생성은 Item provider를 호출하지 않고 null payload를 저장한다")
    void createsExpWithoutItemLookup() {
        var result = service.create(new RewardDefinitionCommand.Create(
                "RD_EXP", "EXP", RewardType.EXP, 10L, null, true
        ));

        assertThat(result.itemId()).isNull();
        assertThat(result.itemCode()).isNull();
        verify(itemLookupApi, never()).getById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("ITEM 수정도 provider code를 다시 snapshot한다")
    void updatesItemSnapshot() {
        RewardDefinition definition = RewardDefinition.create(
                "RD_ITEM", "Item", RewardType.ITEM, 1L, 70L, "IT_OLD", true
        );
        given(definitionReader.getByIdOrThrow(1L)).willReturn(definition);
        given(itemLookupApi.getById(77L)).willReturn(
                new ItemLookupApi.ItemReference(77L, "IT_NEW")
        );

        var result = service.update(1L, new RewardDefinitionCommand.Update(
                "RD_ITEM", "Item", RewardType.ITEM, 3L, 77L, true
        ));

        assertThat(result.amount()).isEqualTo(3L);
        assertThat(result.itemId()).isEqualTo(77L);
        assertThat(result.itemCode()).isEqualTo("IT_NEW");
    }

    @Test
    @DisplayName("provider가 다른 id를 반환하면 stable mismatch로 거부한다")
    void rejectsResolvedIdMismatch() {
        given(itemLookupApi.getById(77L)).willReturn(
                new ItemLookupApi.ItemReference(78L, "IT_STABLE")
        );

        assertError(
                () -> service.create(new RewardDefinitionCommand.Create(
                        "RD_ITEM", "Item", RewardType.ITEM, 1L, 77L, true
                )),
                RewardError.REWARD_ITEM_REFERENCE_INCONSISTENT
        );
    }

    @Test
    @DisplayName("provider의 blank/too-long code를 Reward stable error로 거부한다")
    void rejectsInvalidResolvedCode() {
        given(itemLookupApi.getById(77L)).willReturn(
                new ItemLookupApi.ItemReference(77L, " "),
                new ItemLookupApi.ItemReference(77L, "I".repeat(81))
        );

        assertError(
                () -> service.create(itemCommand()),
                RewardError.REWARD_ITEM_CODE_REQUIRED
        );
        assertError(
                () -> service.create(itemCommand()),
                RewardError.REWARD_ITEM_CODE_TOO_LONG
        );
    }

    private RewardDefinitionCommand.Create itemCommand() {
        return new RewardDefinitionCommand.Create(
                "RD_ITEM", "Item", RewardType.ITEM, 1L, 77L, true
        );
    }

    private void assertError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
