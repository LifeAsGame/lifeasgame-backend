package online.lifeasgame.reward.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RewardSettlement")
class RewardSettlementTest {

    @Nested
    @DisplayName("활성 RewardProfile로 정산을 생성할 때")
    class CreateSettlement {

        @Test
        @DisplayName("Profile Line을 독립된 Settlement Line 스냅샷으로 생성한다")
        void createsLineSnapshots() {
            RewardProfile profile = profileWithExpAndItem();

            RewardSettlement settlement = settlement(profile);

            assertThat(settlement.getRewardProfileId()).isEqualTo(100L);
            assertThat(settlement.getRewardProfileCode()).isEqualTo("RP_REWARD");
            assertThat(settlement.getLines())
                    .extracting(
                            RewardSettlementLine::getRewardDefinitionCode,
                            RewardSettlementLine::getRewardType,
                            RewardSettlementLine::getAmount,
                            RewardSettlementLine::getItemId,
                            RewardSettlementLine::getSortOrder
                    )
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("RD_EXP", RewardType.EXP, 10L, null, 0),
                            org.assertj.core.groups.Tuple.tuple("RD_ITEM", RewardType.ITEM, 2L, 77L, 1)
                    );
            assertThat(settlement.getLines())
                    .extracting(RewardSettlementLine::getStatus)
                    .containsOnly(RewardSettlementLineStatus.PENDING);
            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PENDING);
        }

        @Test
        @DisplayName("amountOverride가 있으면 effectiveAmount를 스냅샷으로 저장한다")
        void snapshotsEffectiveAmount() {
            RewardProfile profile = persistedProfile("RP_OVERRIDE");
            profile.addLine(persistedExpDefinition(1L, "RD_EXP_10", 10L), 0, 30L);

            RewardSettlement settlement = settlement(profile);

            assertThat(settlement.getLines().getFirst().getAmount()).isEqualTo(30L);
        }

        @Test
        @DisplayName("원본 Profile과 Definition이 변경돼도 스냅샷 값은 유지된다")
        void keepsSnapshotAfterSourceChanges() {
            RewardProfile profile = persistedProfile("RP_SNAPSHOT");
            RewardDefinition definition = persistedExpDefinition(1L, "RD_EXP_10", 10L);
            profile.addLine(definition, 0, null);
            RewardSettlement settlement = settlement(profile);

            ReflectionTestUtils.setField(profile, "code", "RP_CHANGED");
            ReflectionTestUtils.setField(definition, "code", "RD_CHANGED");
            ReflectionTestUtils.setField(definition, "amount", 999L);

            RewardSettlementLine line = settlement.getLines().getFirst();
            assertThat(settlement.getRewardProfileCode()).isEqualTo("RP_SNAPSHOT");
            assertThat(line.getRewardDefinitionCode()).isEqualTo("RD_EXP_10");
            assertThat(line.getAmount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Profile Line이 없으면 빈 Line과 COMPLETED 상태로 생성한다")
        void createsCompletedSettlementForEmptyProfile() {
            RewardProfile profile = persistedProfile("RP_NONE");

            RewardSettlement settlement = settlement(profile);

            assertThat(settlement.getRewardProfileId()).isEqualTo(100L);
            assertThat(settlement.getRewardProfileCode()).isEqualTo("RP_NONE");
            assertThat(settlement.getLines()).isEmpty();
            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("실패 Line의 재시도를 준비할 때")
    class PrepareLineRetry {

        @Test
        @DisplayName("FAILED Line을 PENDING으로 바꾸고 failureCode를 제거한다")
        void preparesFailedLine() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());
            settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND);

            boolean changed = settlement.prepareLineRetry(1000L);

            RewardSettlementLine line = settlement.getLineByIdOrThrow(1000L);
            assertThat(changed).isTrue();
            assertThat(line.getStatus()).isEqualTo(RewardSettlementLineStatus.PENDING);
            assertThat(line.getFailureCode()).isNull();
            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PENDING);
        }

        @Test
        @DisplayName("PENDING Line 재호출은 상태를 변경하지 않는다")
        void keepsPendingLine() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            boolean changed = settlement.prepareLineRetry(1000L);

            assertThat(changed).isFalse();
            assertThat(settlement.getLineByIdOrThrow(1000L).getStatus())
                    .isEqualTo(RewardSettlementLineStatus.PENDING);
            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PENDING);
        }

        @Test
        @DisplayName("SUCCEEDED Line은 재시도를 준비할 수 없다")
        void rejectsSucceededLine() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());
            settlement.markLineSucceeded(0);

            assertRewardError(
                    () -> settlement.prepareLineRetry(1000L),
                    RewardError.REWARD_SETTLEMENT_SUCCEEDED_LINE_CANNOT_RETRY
            );
        }

        @Test
        @DisplayName("부분 실패의 대상 Line만 PENDING으로 바꾸고 부모 상태를 재계산한다")
        void recalculatesPartialFailure() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());
            settlement.markLineSucceeded(0);
            settlement.markLineFailed(1, RewardError.REWARD_DEFINITION_NOT_FOUND);
            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PARTIAL_FAILED);

            settlement.prepareLineRetry(1001L);

            assertThat(settlement.getLineByIdOrThrow(1000L).getStatus())
                    .isEqualTo(RewardSettlementLineStatus.SUCCEEDED);
            assertThat(settlement.getLineByIdOrThrow(1001L).getStatus())
                    .isEqualTo(RewardSettlementLineStatus.PENDING);
            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PENDING);
        }

        @Test
        @DisplayName("전체 실패에서 한 Line을 준비하면 부모 상태가 PENDING이 된다")
        void recalculatesTotalFailure() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());
            settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND);
            settlement.markLineFailed(1, RewardError.REWARD_DEFINITION_NOT_FOUND);
            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.FAILED);

            settlement.prepareLineRetry(1000L);

            assertThat(settlement.getLineByIdOrThrow(1000L).getStatus())
                    .isEqualTo(RewardSettlementLineStatus.PENDING);
            assertThat(settlement.getLineByIdOrThrow(1001L).getStatus())
                    .isEqualTo(RewardSettlementLineStatus.FAILED);
            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("EXP Line 처리 가능 여부를 확인할 때")
    class ValidateExpLineProcessing {

        @Test
        @DisplayName("PENDING EXP Line은 처리가 필요하다")
        void requiresPendingExpProcessing() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            assertThat(settlement.getLineOrThrow(0).isExpProcessingRequired()).isTrue();
        }

        @Test
        @DisplayName("ITEM Line은 EXP processor에서 처리할 수 없다")
        void rejectsItemLine() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            assertRewardError(
                    () -> settlement.getLineOrThrow(1).isExpProcessingRequired(),
                    RewardError.REWARD_SETTLEMENT_LINE_NOT_EXP
            );
        }

        @Test
        @DisplayName("SUCCEEDED EXP Line은 기존 결과를 유지하고 재처리하지 않는다")
        void skipsSucceededExpLine() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());
            settlement.markExpLineSucceeded(1000L);

            boolean processingRequired = settlement.getLineOrThrow(0).isExpProcessingRequired();

            assertThat(processingRequired).isFalse();
            assertThat(settlement.getLineByIdOrThrow(1000L).getStatus())
                    .isEqualTo(RewardSettlementLineStatus.SUCCEEDED);
        }

        @Test
        @DisplayName("FAILED EXP Line은 자동 재처리할 수 없다")
        void rejectsFailedExpLine() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());
            settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND);

            assertRewardError(
                    () -> settlement.getLineOrThrow(0).isExpProcessingRequired(),
                    RewardError.REWARD_SETTLEMENT_LINE_ALREADY_FAILED
            );
        }

        @Test
        @DisplayName("다른 Settlement의 lineId는 소유 Line으로 조회하지 않는다")
        void rejectsForeignLineId() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            assertRewardError(
                    () -> settlement.getLineByIdOrThrow(9999L),
                    RewardError.REWARD_SETTLEMENT_LINE_NOT_FOUND
            );
        }
    }

    @Nested
    @DisplayName("Line 처리 결과를 반영할 때")
    class UpdateLineResult {

        @Test
        @DisplayName("성공 처리한 Line은 SUCCEEDED가 되고 같은 성공 처리는 멱등이다")
        void succeedsIdempotently() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            settlement.markLineSucceeded(0);
            settlement.markLineSucceeded(0);

            assertThat(settlement.getLines().getFirst().getStatus())
                    .isEqualTo(RewardSettlementLineStatus.SUCCEEDED);
        }

        @Test
        @DisplayName("실패 처리한 Line은 FAILED와 ErrorCode를 저장한다")
        void storesFailureCode() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND);

            RewardSettlementLine line = settlement.getLines().getFirst();
            assertThat(line.getStatus()).isEqualTo(RewardSettlementLineStatus.FAILED);
            assertThat(line.getFailureCode())
                    .isEqualTo(RewardError.REWARD_DEFINITION_NOT_FOUND.code());
        }

        @Test
        @DisplayName("성공한 Line을 실패 처리하면 도메인 예외가 발생한다")
        void rejectsFailureAfterSuccess() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());
            settlement.markLineSucceeded(0);

            assertRewardError(
                    () -> settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND),
                    RewardError.REWARD_SETTLEMENT_SUCCEEDED_LINE_CANNOT_FAIL
            );
        }

        @Test
        @DisplayName("실패한 Line을 성공 처리하면 도메인 예외가 발생한다")
        void rejectsSuccessAfterFailure() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());
            settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND);

            assertRewardError(
                    () -> settlement.markLineSucceeded(0),
                    RewardError.REWARD_SETTLEMENT_LINE_ALREADY_FAILED
            );
        }

        @Test
        @DisplayName("실패 ErrorCode가 없으면 도메인 예외가 발생한다")
        void requiresFailureCode() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            assertRewardError(
                    () -> settlement.markLineFailed(0, null),
                    RewardError.REWARD_SETTLEMENT_FAILURE_CODE_REQUIRED
            );
        }
    }

    @Nested
    @DisplayName("Line 상태로 Settlement 상태를 계산할 때")
    class CalculateSettlementStatus {

        @Test
        @DisplayName("모든 Line이 성공하면 COMPLETED가 된다")
        void completesWhenAllLinesSucceed() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            settlement.markLineSucceeded(0);
            settlement.markLineSucceeded(1);

            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.COMPLETED);
        }

        @Test
        @DisplayName("모든 Line이 실패하면 FAILED가 된다")
        void failsWhenAllLinesFail() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND);
            settlement.markLineFailed(1, RewardError.REWARD_DEFINITION_NOT_FOUND);

            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.FAILED);
        }

        @Test
        @DisplayName("성공과 실패 Line이 함께 있으면 PARTIAL_FAILED가 된다")
        void partiallyFailsWhenResultsAreMixed() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            settlement.markLineSucceeded(0);
            settlement.markLineFailed(1, RewardError.REWARD_DEFINITION_NOT_FOUND);

            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PARTIAL_FAILED);
        }

        @Test
        @DisplayName("하나라도 Pending Line이 남으면 PENDING을 유지한다")
        void remainsPendingWhileLineIsPending() {
            RewardSettlement settlement = settlement(profileWithExpAndItem());

            settlement.markLineSucceeded(0);

            assertThat(settlement.getStatus()).isEqualTo(RewardSettlementStatus.PENDING);
        }
    }

    private RewardSettlement settlement(RewardProfile profile) {
        RewardSettlement settlement = RewardSettlement.create(
                1L,
                RewardSettlementSourceType.QUEST_COMPLETION,
                1000L,
                profile
        );
        for (int index = 0; index < settlement.getLines().size(); index++) {
            ReflectionTestUtils.setField(settlement.getLines().get(index), "id", 1000L + index);
        }
        return settlement;
    }

    private RewardProfile profileWithExpAndItem() {
        RewardProfile profile = persistedProfile("RP_REWARD");
        profile.addLine(persistedExpDefinition(1L, "RD_EXP", 10L), 0, null);
        profile.addLine(persistedItemDefinition(2L, "RD_ITEM", 77L, 2L), 1, null);
        return profile;
    }

    private RewardProfile persistedProfile(String code) {
        RewardProfile profile = RewardProfile.create(code, code, RewardProfileStatus.ACTIVE);
        ReflectionTestUtils.setField(profile, "id", 100L);
        return profile;
    }

    private RewardDefinition persistedExpDefinition(Long id, String code, Long amount) {
        RewardDefinition definition = RewardDefinition.create(
                code, code, RewardType.EXP, amount, null, true
        );
        ReflectionTestUtils.setField(definition, "id", id);
        return definition;
    }

    private RewardDefinition persistedItemDefinition(
            Long id,
            String code,
            Long itemId,
            Long quantity
    ) {
        RewardDefinition definition = RewardDefinition.create(
                code, code, RewardType.ITEM, quantity, itemId, true
        );
        ReflectionTestUtils.setField(definition, "id", id);
        return definition;
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
