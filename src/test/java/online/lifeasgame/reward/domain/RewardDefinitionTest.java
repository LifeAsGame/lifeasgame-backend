package online.lifeasgame.reward.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RewardDefinition")
class RewardDefinitionTest {

    @Nested
    @DisplayName("EXP 보상 정의를 생성할 때")
    class CreateExpRewardDefinition {

        @Test
        @DisplayName("양수 amount와 itemId 없이 생성된다")
        void createsWithAmountAndWithoutItemId() {
            RewardDefinition definition = RewardDefinition.create(
                    "RD_EXP_10", "EXP 10", RewardType.EXP, 10L, null, null, true
            );

            assertThat(definition.getRewardType()).isEqualTo(RewardType.EXP);
            assertThat(definition.getAmount()).isEqualTo(10L);
            assertThat(definition.getItemId()).isNull();
            assertThat(definition.getItemCode()).isNull();
            assertThat(definition.isActive()).isTrue();
        }

        @Test
        @DisplayName("amount가 없으면 REWARD_AMOUNT_MUST_BE_POSITIVE 예외가 발생한다")
        void throwsWhenAmountIsMissing() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_EXP", "EXP", RewardType.EXP, null, null, null, true
                    ),
                    RewardError.REWARD_AMOUNT_MUST_BE_POSITIVE
            );
        }

        @Test
        @DisplayName("itemId가 있으면 REWARD_EXP_ITEM_ID_NOT_ALLOWED 예외가 발생한다")
        void throwsWhenItemIdExists() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_EXP", "EXP", RewardType.EXP, 10L, 1L, null, true
                    ),
                    RewardError.REWARD_EXP_ITEM_ID_NOT_ALLOWED
            );
        }
    }

    @Nested
    @DisplayName("ITEM 보상 정의를 생성할 때")
    class CreateItemRewardDefinition {

        @Test
        @DisplayName("itemId와 수량을 의미하는 amount로 생성된다")
        void createsWithItemIdAndAmount() {
            RewardDefinition definition = RewardDefinition.create(
                    "RD_ITEM_1", "Item 1", RewardType.ITEM, 2L, 1L, "  IT_ITEM_1  ", true
            );

            assertThat(definition.getRewardType()).isEqualTo(RewardType.ITEM);
            assertThat(definition.getAmount()).isEqualTo(2L);
            assertThat(definition.getItemId()).isEqualTo(1L);
            assertThat(definition.getItemCode()).isEqualTo("IT_ITEM_1");
        }

        @Test
        @DisplayName("itemId가 없으면 REWARD_ITEM_ID_REQUIRED 예외가 발생한다")
        void throwsWhenItemIdIsMissing() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_ITEM", "Item", RewardType.ITEM, 1L, null, "IT_ITEM", true
                    ),
                    RewardError.REWARD_ITEM_ID_REQUIRED
            );
        }

        @Test
        @DisplayName("수량이 양수가 아니면 REWARD_ITEM_QUANTITY_MUST_BE_POSITIVE 예외가 발생한다")
        void throwsWhenAmountIsNotPositive() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_ITEM", "Item", RewardType.ITEM, 0L, 1L, "IT_ITEM", true
                    ),
                    RewardError.REWARD_ITEM_QUANTITY_MUST_BE_POSITIVE
            );
        }

        @Test
        @DisplayName("itemId가 양수가 아니면 REWARD_ITEM_ID_MUST_BE_POSITIVE 예외가 발생한다")
        void throwsWhenItemIdIsNotPositive() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_ITEM", "Item", RewardType.ITEM, 1L, 0L, "IT_ITEM", true
                    ),
                    RewardError.REWARD_ITEM_ID_MUST_BE_POSITIVE
            );
        }

        @Test
        @DisplayName("itemCode가 공백이면 REWARD_ITEM_CODE_REQUIRED 예외가 발생한다")
        void throwsWhenItemCodeIsBlank() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_ITEM", "Item", RewardType.ITEM, 1L, 1L, " ", true
                    ),
                    RewardError.REWARD_ITEM_CODE_REQUIRED
            );
        }

        @Test
        @DisplayName("itemCode가 80자를 넘으면 REWARD_ITEM_CODE_TOO_LONG 예외가 발생한다")
        void throwsWhenItemCodeIsTooLong() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_ITEM", "Item", RewardType.ITEM, 1L, 1L,
                            "I".repeat(81), true
                    ),
                    RewardError.REWARD_ITEM_CODE_TOO_LONG
            );
        }
    }

    @Nested
    @DisplayName("필수 정의 값을 검증할 때")
    class ValidateRequiredDefinitionValues {

        @Test
        @DisplayName("code와 name의 앞뒤 공백을 제거해 저장한다")
        void trimsCodeAndName() {
            RewardDefinition definition = RewardDefinition.create(
                    "  RD_EXP  ", "  EXP  ", RewardType.EXP, 1L, null, null, true
            );

            assertThat(definition.getCode()).isEqualTo("RD_EXP");
            assertThat(definition.getName()).isEqualTo("EXP");
        }

        @Test
        @DisplayName("code가 공백이면 REWARD_DEFINITION_CODE_REQUIRED 예외가 발생한다")
        void throwsWhenCodeIsBlank() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            " ", "EXP", RewardType.EXP, 1L, null, null, true
                    ),
                    RewardError.REWARD_DEFINITION_CODE_REQUIRED
            );
        }

        @Test
        @DisplayName("name이 공백이면 REWARD_DEFINITION_NAME_REQUIRED 예외가 발생한다")
        void throwsWhenNameIsBlank() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_EXP", " ", RewardType.EXP, 1L, null, null, true
                    ),
                    RewardError.REWARD_DEFINITION_NAME_REQUIRED
            );
        }

        @Test
        @DisplayName("rewardType이 없으면 REWARD_LINE_TYPE_REQUIRED 예외가 발생한다")
        void throwsWhenRewardTypeIsMissing() {
            assertRewardError(
                    () -> RewardDefinition.create(
                            "RD_EXP", "EXP", null, 1L, null, null, true
                    ),
                    RewardError.REWARD_LINE_TYPE_REQUIRED
            );
        }
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
