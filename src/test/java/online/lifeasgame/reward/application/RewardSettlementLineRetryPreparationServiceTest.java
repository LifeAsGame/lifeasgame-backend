package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.result.RewardSettlementLineRetryPreparationResult;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.RewardProfileStatus;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import online.lifeasgame.reward.domain.RewardType;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardSettlementLineRetryPreparationService")
class RewardSettlementLineRetryPreparationServiceTest {

    private static final Long SETTLEMENT_ID = 189L;
    private static final Long LINE_ID = 1890L;

    @Mock
    private RewardSettlementReader settlementReader;

    @Mock
    private RewardSettlementWriter settlementWriter;

    private RewardSettlementLineRetryPreparationService service;

    @BeforeEach
    void setUp() {
        service = new RewardSettlementLineRetryPreparationService(
                settlementReader, settlementWriter
        );
    }

    @Nested
    @DisplayName("FAILED Line의 재시도를 준비할 때")
    class PrepareFailedLine {

        @Test
        @DisplayName("Settlement를 잠금 조회하고 변경 상태를 flush한다")
        void locksAndFlushesChangedSettlement() {
            RewardSettlement settlement = settlement();
            settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND);
            given(settlementReader.getByIdForUpdateOrThrow(SETTLEMENT_ID))
                    .willReturn(settlement);

            RewardSettlementLineRetryPreparationResult result =
                    service.prepare(SETTLEMENT_ID, LINE_ID);

            assertThat(result.settlementId()).isEqualTo(SETTLEMENT_ID);
            assertThat(result.lineId()).isEqualTo(LINE_ID);
            assertThat(result.lineStatus()).isEqualTo(RewardSettlementLineStatus.PENDING);
            assertThat(result.settlementStatus()).isEqualTo(RewardSettlementStatus.PENDING);
            assertThat(result.changed()).isTrue();
            assertThat(settlement.getLineByIdOrThrow(LINE_ID).getFailureCode()).isNull();
            verify(settlementReader).getByIdForUpdateOrThrow(SETTLEMENT_ID);
            verify(settlementWriter).saveAndFlush(settlement);
        }
    }

    @Nested
    @DisplayName("PENDING Line의 재시도를 준비할 때")
    class PreparePendingLine {

        @Test
        @DisplayName("no-op 결과를 반환하고 저장하지 않는다")
        void returnsNoOpWithoutSave() {
            RewardSettlement settlement = settlement();
            given(settlementReader.getByIdForUpdateOrThrow(SETTLEMENT_ID))
                    .willReturn(settlement);

            RewardSettlementLineRetryPreparationResult result =
                    service.prepare(SETTLEMENT_ID, LINE_ID);

            assertThat(result.lineStatus()).isEqualTo(RewardSettlementLineStatus.PENDING);
            assertThat(result.settlementStatus()).isEqualTo(RewardSettlementStatus.PENDING);
            assertThat(result.changed()).isFalse();
            verify(settlementWriter, never()).saveAndFlush(settlement);
        }
    }

    @Nested
    @DisplayName("재시도 대상을 검증할 때")
    class ValidateRetryTarget {

        @Test
        @DisplayName("Settlement가 없으면 안정된 DomainException을 전파한다")
        void propagatesMissingSettlementError() {
            given(settlementReader.getByIdForUpdateOrThrow(SETTLEMENT_ID))
                    .willThrow(new DomainException(RewardError.REWARD_SETTLEMENT_NOT_FOUND));

            assertRewardError(
                    () -> service.prepare(SETTLEMENT_ID, LINE_ID),
                    RewardError.REWARD_SETTLEMENT_NOT_FOUND
            );
            verify(settlementWriter, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("Settlement에 속하지 않은 Line이면 저장하지 않는다")
        void rejectsMissingLine() {
            RewardSettlement settlement = settlement();
            given(settlementReader.getByIdForUpdateOrThrow(SETTLEMENT_ID))
                    .willReturn(settlement);

            assertRewardError(
                    () -> service.prepare(SETTLEMENT_ID, 9999L),
                    RewardError.REWARD_SETTLEMENT_LINE_NOT_FOUND
            );
            verify(settlementWriter, never()).saveAndFlush(settlement);
        }

        @Test
        @DisplayName("SUCCEEDED Line이면 저장하지 않고 Retry 거부 예외를 전파한다")
        void rejectsSucceededLine() {
            RewardSettlement settlement = settlement();
            settlement.markLineSucceeded(0);
            given(settlementReader.getByIdForUpdateOrThrow(SETTLEMENT_ID))
                    .willReturn(settlement);

            assertRewardError(
                    () -> service.prepare(SETTLEMENT_ID, LINE_ID),
                    RewardError.REWARD_SETTLEMENT_SUCCEEDED_LINE_CANNOT_RETRY
            );
            verify(settlementWriter, never()).saveAndFlush(settlement);
        }
    }

    private RewardSettlement settlement() {
        RewardProfile profile = RewardProfile.create(
                "RP_EXP_10", "EXP 10 Profile", RewardProfileStatus.ACTIVE
        );
        ReflectionTestUtils.setField(profile, "id", 10L);
        RewardDefinition definition = RewardDefinition.create(
                "RD_EXP_10", "EXP 10", RewardType.EXP, 10L, null, true
        );
        ReflectionTestUtils.setField(definition, "id", 20L);
        profile.addLine(definition, 0, null);
        RewardSettlement settlement = RewardSettlement.create(
                1L,
                RewardSettlementSourceType.QUEST_COMPLETION,
                18900L,
                profile
        );
        ReflectionTestUtils.setField(settlement, "id", SETTLEMENT_ID);
        ReflectionTestUtils.setField(settlement.getLines().getFirst(), "id", LINE_ID);
        return settlement;
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
