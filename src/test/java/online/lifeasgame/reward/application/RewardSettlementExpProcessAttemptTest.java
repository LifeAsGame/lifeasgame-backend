package online.lifeasgame.reward.application;

import online.lifeasgame.character.application.internal.PlayerGrowthApi;
import online.lifeasgame.character.application.internal.PlayerGrowthApi.PlayerGrowthGrantResult;
import online.lifeasgame.core.error.DomainException;
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
@DisplayName("RewardSettlementExpProcessAttempt")
class RewardSettlementExpProcessAttemptTest {

    @Mock
    private RewardSettlementReader settlementReader;

    @Mock
    private RewardSettlementWriter settlementWriter;

    @Mock
    private PlayerGrowthApi playerGrowthApi;

    private RewardSettlementExpProcessAttempt attempt;

    @BeforeEach
    void setUp() {
        attempt = new RewardSettlementExpProcessAttempt(
                settlementReader,
                settlementWriter,
                playerGrowthApi
        );
    }

    @Nested
    @DisplayName("PENDING EXP Line을 처리할 때")
    class ProcessPendingLine {

        @Test
        @DisplayName("Player EXP 지급과 Line 성공을 하나의 시도로 반영한다")
        void grantsExpAndSucceedsLine() {
            RewardSettlement settlement = settlementWithExpOnly();
            given(settlementReader.getByIdForUpdateOrThrow(100L)).willReturn(settlement);
            given(playerGrowthApi.grantRewardExp(1L, 1000L, 10L)).willReturn(grantResult(false));
            given(settlementWriter.saveAndFlush(settlement)).willReturn(settlement);

            var result = attempt.process(100L, 1000L);

            assertThat(result.settlementId()).isEqualTo(100L);
            assertThat(result.lineStatus()).isEqualTo(RewardSettlementLineStatus.SUCCEEDED);
            assertThat(result.appliedExp()).isEqualTo(10L);
            assertThat(result.beforeLevel()).isEqualTo(1);
            assertThat(result.afterLevel()).isEqualTo(1);
            assertThat(result.beforeTotalExp()).isZero();
            assertThat(result.afterTotalExp()).isEqualTo(10L);
            assertThat(result.settlementStatus()).isEqualTo(RewardSettlementStatus.COMPLETED);
            assertThat(result.replayed()).isFalse();
            assertThat(result.growthChangeId()).isEqualTo(500L);
            verify(settlementWriter).saveAndFlush(settlement);
        }

        @Test
        @DisplayName("다른 PENDING Line이 남아 있으면 Settlement는 PENDING을 유지한다")
        void keepsSettlementPending() {
            RewardSettlement settlement = settlementWithExpAndItem();
            given(settlementReader.getByIdForUpdateOrThrow(100L)).willReturn(settlement);
            given(playerGrowthApi.grantRewardExp(1L, 1000L, 10L)).willReturn(grantResult(false));
            given(settlementWriter.saveAndFlush(settlement)).willReturn(settlement);

            var result = attempt.process(100L, 1000L);

            assertThat(result.settlementStatus()).isEqualTo(RewardSettlementStatus.PENDING);
        }

        @Test
        @DisplayName("PENDING Line에 기존 GrowthChange가 있으면 invariant error로 중단한다")
        void rejectsGrowthReplayForPendingLine() {
            RewardSettlement settlement = settlementWithExpOnly();
            given(settlementReader.getByIdForUpdateOrThrow(100L)).willReturn(settlement);
            given(playerGrowthApi.grantRewardExp(1L, 1000L, 10L)).willReturn(grantResult(true));

            assertRewardError(
                    () -> attempt.process(100L, 1000L),
                    RewardError.REWARD_SETTLEMENT_EXP_GROWTH_INCONSISTENT
            );
            assertThat(settlement.getLineByIdOrThrow(1000L).getStatus())
                    .isEqualTo(RewardSettlementLineStatus.PENDING);
            verify(settlementWriter, never()).saveAndFlush(settlement);
        }
    }

    @Nested
    @DisplayName("처리할 수 없는 Line을 요청할 때")
    class RejectInvalidLine {

        @Test
        @DisplayName("ITEM Line은 RewardError로 거부하고 Player API를 호출하지 않는다")
        void rejectsItemLine() {
            RewardSettlement settlement = settlementWithExpAndItem();
            given(settlementReader.getByIdForUpdateOrThrow(100L)).willReturn(settlement);

            assertRewardError(
                    () -> attempt.process(100L, 1001L),
                    RewardError.REWARD_SETTLEMENT_LINE_NOT_EXP
            );
            verify(playerGrowthApi, never()).grantRewardExp(1L, 1001L, 2L);
        }

        @Test
        @DisplayName("FAILED Line은 자동 재처리를 거부한다")
        void rejectsFailedLine() {
            RewardSettlement settlement = settlementWithExpOnly();
            settlement.markLineFailed(0, RewardError.REWARD_DEFINITION_NOT_FOUND);
            given(settlementReader.getByIdForUpdateOrThrow(100L)).willReturn(settlement);

            assertRewardError(
                    () -> attempt.process(100L, 1000L),
                    RewardError.REWARD_SETTLEMENT_LINE_ALREADY_FAILED
            );
            verify(playerGrowthApi, never()).grantRewardExp(1L, 1000L, 10L);
        }
    }

    @Nested
    @DisplayName("이미 SUCCEEDED인 EXP Line을 다시 요청할 때")
    class ProcessSucceededLine {

        @Test
        @DisplayName("Player API를 호출하지 않고 저장된 기존 결과를 반환한다")
        void returnsExistingResultWithoutGrant() {
            RewardSettlement settlement = settlementWithExpOnly();
            settlement.markExpLineSucceeded(1000L);
            given(settlementReader.getByIdForUpdateOrThrow(100L)).willReturn(settlement);
            given(playerGrowthApi.findRewardExpGrant(1000L)).willReturn(
                    java.util.Optional.of(grantResult(true))
            );

            var result = attempt.process(100L, 1000L);

            assertThat(result.replayed()).isTrue();
            assertThat(result.appliedExp()).isEqualTo(10L);
            assertThat(result.settlementStatus()).isEqualTo(RewardSettlementStatus.COMPLETED);
            verify(playerGrowthApi, never()).grantRewardExp(1L, 1000L, 10L);
            verify(settlementWriter, never()).saveAndFlush(settlement);
        }

        @Test
        @DisplayName("SUCCEEDED Line에 GrowthChange가 없으면 invariant error로 중단한다")
        void rejectsMissingGrowthChange() {
            RewardSettlement settlement = settlementWithExpOnly();
            settlement.markExpLineSucceeded(1000L);
            given(settlementReader.getByIdForUpdateOrThrow(100L)).willReturn(settlement);
            given(playerGrowthApi.findRewardExpGrant(1000L)).willReturn(java.util.Optional.empty());

            assertRewardError(
                    () -> attempt.process(100L, 1000L),
                    RewardError.REWARD_SETTLEMENT_EXP_GROWTH_INCONSISTENT
            );
            verify(playerGrowthApi, never()).grantRewardExp(1L, 1000L, 10L);
        }
    }

    private PlayerGrowthGrantResult grantResult(boolean replayed) {
        return new PlayerGrowthGrantResult(
                500L, 1L, 1000L, 10L, 10L, 0L,
                1, 1, 0L, 10L, replayed
        );
    }

    private RewardSettlement settlementWithExpOnly() {
        RewardProfile profile = profile();
        profile.addLine(expDefinition(), 0, null);
        return settlement(profile);
    }

    private RewardSettlement settlementWithExpAndItem() {
        RewardProfile profile = profile();
        profile.addLine(expDefinition(), 0, null);
        RewardDefinition item = RewardDefinition.create(
                "RD_ITEM", "Item", RewardType.ITEM, 2L, 77L, "IT_ITEM", true
        );
        ReflectionTestUtils.setField(item, "id", 3L);
        profile.addLine(item, 1, null);
        return settlement(profile);
    }

    private RewardProfile profile() {
        RewardProfile profile = RewardProfile.create("RP", "Profile", RewardProfileStatus.ACTIVE);
        ReflectionTestUtils.setField(profile, "id", 2L);
        return profile;
    }

    private RewardDefinition expDefinition() {
        RewardDefinition definition = RewardDefinition.create(
                "RD_EXP", "EXP", RewardType.EXP, 10L, null, null, true
        );
        ReflectionTestUtils.setField(definition, "id", 1L);
        return definition;
    }

    private RewardSettlement settlement(RewardProfile profile) {
        RewardSettlement settlement = RewardSettlement.create(
                1L, RewardSettlementSourceType.QUEST_COMPLETION, 10L, profile
        );
        ReflectionTestUtils.setField(settlement, "id", 100L);
        for (int index = 0; index < settlement.getLines().size(); index++) {
            ReflectionTestUtils.setField(settlement.getLines().get(index), "id", 1000L + index);
        }
        return settlement;
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
