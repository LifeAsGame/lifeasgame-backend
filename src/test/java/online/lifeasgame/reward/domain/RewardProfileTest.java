package online.lifeasgame.reward.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RewardProfile")
class RewardProfileTest {

    @Nested
    @DisplayName("보상 라인을 추가할 때")
    class AddRewardLine {

        @Test
        @DisplayName("EXP 라인을 profile에 포함한다")
        void includesExpLine() {
            RewardProfile profile = activeProfile();
            RewardDefinition definition = expDefinition("RD_EXP_10", 10L);

            RewardProfileLine line = profile.addLine(definition, 0, null);

            assertThat(profile.getLines()).containsExactly(line);
            assertThat(line.getRewardDefinition()).isSameAs(definition);
            assertThat(line.effectiveAmount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("ITEM 라인을 profile에 포함한다")
        void includesItemLine() {
            RewardProfile profile = activeProfile();
            RewardDefinition definition = RewardDefinition.create(
                    "RD_ITEM_1", "Item 1", RewardType.ITEM, 1L, 1L, true
            );

            RewardProfileLine line = profile.addLine(definition, 0, 3L);

            assertThat(line.getRewardDefinition().getRewardType()).isEqualTo(RewardType.ITEM);
            assertThat(line.getAmountOverride()).isEqualTo(3L);
            assertThat(line.effectiveAmount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("sortOrder 오름차순으로 line을 반환한다")
        void returnsLinesOrderedBySortOrder() {
            RewardProfile profile = activeProfile();
            RewardProfileLine later = profile.addLine(expDefinition("RD_EXP_30", 30L), 20, null);
            RewardProfileLine earlier = profile.addLine(expDefinition("RD_EXP_10", 10L), 10, null);

            assertThat(profile.getLines()).containsExactly(earlier, later);
        }

        @Test
        @DisplayName("같은 sortOrder를 추가하면 REWARD_LINE_SORT_ORDER_DUPLICATED 예외가 발생한다")
        void throwsWhenSortOrderIsDuplicated() {
            RewardProfile profile = activeProfile();
            profile.addLine(expDefinition("RD_EXP_10", 10L), 0, null);

            assertThatThrownBy(() ->
                    profile.addLine(expDefinition("RD_EXP_30", 30L), 0, null)
            ).isInstanceOfSatisfying(DomainException.class, exception ->
                    assertThat(exception.getErrorCode())
                            .isEqualTo(RewardError.REWARD_LINE_SORT_ORDER_DUPLICATED)
            );
        }
    }

    @Nested
    @DisplayName("잘못된 보상 라인을 추가할 때")
    class AddInvalidRewardLine {

        @Test
        @DisplayName("RewardDefinition이 없으면 REWARD_LINE_TARGET_REQUIRED 예외가 발생한다")
        void throwsWhenDefinitionIsMissing() {
            assertRewardError(
                    () -> activeProfile().addLine(null, 0, null),
                    RewardError.REWARD_LINE_TARGET_REQUIRED
            );
        }

        @Test
        @DisplayName("sortOrder가 음수이면 REWARD_LINE_SORT_ORDER_MUST_BE_NON_NEGATIVE 예외가 발생한다")
        void throwsWhenSortOrderIsNegative() {
            assertRewardError(
                    () -> activeProfile().addLine(expDefinition("RD_EXP", 1L), -1, null),
                    RewardError.REWARD_LINE_SORT_ORDER_MUST_BE_NON_NEGATIVE
            );
        }

        @Test
        @DisplayName("override가 양수가 아니면 REWARD_AMOUNT_OVERRIDE_MUST_BE_POSITIVE 예외가 발생한다")
        void throwsWhenAmountOverrideIsNotPositive() {
            assertRewardError(
                    () -> activeProfile().addLine(expDefinition("RD_EXP", 1L), 0, 0L),
                    RewardError.REWARD_AMOUNT_OVERRIDE_MUST_BE_POSITIVE
            );
        }
    }

    @Nested
    @DisplayName("profile 필수 값을 검증할 때")
    class ValidateRequiredProfileValues {

        @Test
        @DisplayName("code와 name의 앞뒤 공백을 제거해 저장한다")
        void trimsCodeAndName() {
            RewardProfile profile = RewardProfile.create(
                    "  RP_TEST  ", "  Test Profile  ", RewardProfileStatus.ACTIVE
            );

            assertThat(profile.getCode()).isEqualTo("RP_TEST");
            assertThat(profile.getName()).isEqualTo("Test Profile");
        }

        @Test
        @DisplayName("code가 공백이면 REWARD_PROFILE_CODE_REQUIRED 예외가 발생한다")
        void throwsWhenCodeIsBlank() {
            assertRewardError(
                    () -> RewardProfile.create(" ", "Profile", RewardProfileStatus.ACTIVE),
                    RewardError.REWARD_PROFILE_CODE_REQUIRED
            );
        }

        @Test
        @DisplayName("name이 공백이면 REWARD_PROFILE_NAME_REQUIRED 예외가 발생한다")
        void throwsWhenNameIsBlank() {
            assertRewardError(
                    () -> RewardProfile.create("RP_TEST", " ", RewardProfileStatus.ACTIVE),
                    RewardError.REWARD_PROFILE_NAME_REQUIRED
            );
        }
    }

    @Nested
    @DisplayName("profile 활성 상태를 확인할 때")
    class AssertActiveProfile {

        @Test
        @DisplayName("활성 profile이면 예외가 발생하지 않는다")
        void doesNotThrowWhenProfileIsActive() {
            assertThatCode(activeProfile()::assertActive).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("비활성 profile이면 REWARD_PROFILE_INACTIVE 예외가 발생한다")
        void throwsWhenProfileIsInactive() {
            RewardProfile profile = RewardProfile.create(
                    "RP_INACTIVE", "Inactive Profile", RewardProfileStatus.INACTIVE
            );

            assertThatThrownBy(profile::assertActive)
                    .isInstanceOfSatisfying(DomainException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(RewardError.REWARD_PROFILE_INACTIVE)
                    );
        }
    }

    private RewardProfile activeProfile() {
        return RewardProfile.create("RP_TEST", "Test Profile", RewardProfileStatus.ACTIVE);
    }

    private RewardDefinition expDefinition(String code, Long amount) {
        return RewardDefinition.create(code, code, RewardType.EXP, amount, null, true);
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
