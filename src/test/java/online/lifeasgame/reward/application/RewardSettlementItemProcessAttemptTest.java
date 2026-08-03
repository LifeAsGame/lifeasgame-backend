package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryRewardDeliveryApi;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardSettlementItemProcessAttempt")
class RewardSettlementItemProcessAttemptTest {

    @Mock
    private RewardSettlementReader settlementReader;

    @Mock
    private RewardSettlementWriter settlementWriter;

    @Mock
    private InventoryRewardDeliveryApi inventoryDeliveryApi;

    private RewardSettlementItemProcessAttempt attempt;

    @BeforeEach
    void setUp() {
        attempt = new RewardSettlementItemProcessAttempt(
                settlementReader,
                settlementWriter,
                inventoryDeliveryApi
        );
    }

    @Test
    @DisplayName("PENDING ITEM을 신규 지급하고 Line과 Settlement를 성공시킨다")
    void deliversPendingItem() {
        RewardSettlement settlement = itemSettlement();
        given(settlementReader.getByIdForUpdateOrThrow(100L))
                .willReturn(settlement);
        given(inventoryDeliveryApi.deliverReward(
                1000L, 1L, "IT_ITEM", 2L
        )).willReturn(delivery(false));

        var result = attempt.process(100L, 1000L);

        assertThat(result.lineStatus())
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED);
        assertThat(result.settlementStatus())
                .isEqualTo(RewardSettlementStatus.COMPLETED);
        assertThat(result.replayed()).isFalse();
        assertThat(result.deliveryId()).isEqualTo(500L);
        verify(settlementWriter).saveAndFlush(settlement);
    }

    @Test
    @DisplayName("PENDING ITEM의 matching receipt replay로 Line 성공을 복구한다")
    void recoversPendingLineFromDeliveryReplay() {
        RewardSettlement settlement = itemSettlement();
        given(settlementReader.getByIdForUpdateOrThrow(100L))
                .willReturn(settlement);
        given(inventoryDeliveryApi.deliverReward(
                1000L, 1L, "IT_ITEM", 2L
        )).willReturn(delivery(true));

        var result = attempt.process(100L, 1000L);

        assertThat(result.replayed()).isTrue();
        assertThat(result.lineStatus())
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED);
        verify(settlementWriter).saveAndFlush(settlement);
    }

    @Test
    @DisplayName("SUCCEEDED ITEM은 receipt만 검증하고 신규 지급과 저장을 하지 않는다")
    void verifiesSucceededLineReceiptOnly() {
        RewardSettlement settlement = itemSettlement();
        settlement.markItemLineSucceeded(1000L);
        given(settlementReader.getByIdForUpdateOrThrow(100L))
                .willReturn(settlement);
        given(inventoryDeliveryApi.findRewardDelivery(1000L))
                .willReturn(Optional.of(receipt()));

        var result = attempt.process(100L, 1000L);

        assertThat(result.replayed()).isTrue();
        verify(inventoryDeliveryApi, never()).deliverReward(
                1000L, 1L, "IT_ITEM", 2L
        );
        verify(settlementWriter, never()).saveAndFlush(settlement);
    }

    @Test
    @DisplayName("SUCCEEDED ITEM receipt가 없으면 신규 지급 없이 invariant error다")
    void rejectsSucceededLineWithoutReceipt() {
        RewardSettlement settlement = itemSettlement();
        settlement.markItemLineSucceeded(1000L);
        given(settlementReader.getByIdForUpdateOrThrow(100L))
                .willReturn(settlement);
        given(inventoryDeliveryApi.findRewardDelivery(1000L))
                .willReturn(Optional.empty());

        assertInconsistent(() -> attempt.process(100L, 1000L));

        verify(inventoryDeliveryApi, never()).deliverReward(
                1000L, 1L, "IT_ITEM", 2L
        );
        assertThat(settlement.getLineByIdOrThrow(1000L).getStatus())
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED);
    }

    @ParameterizedTest
    @MethodSource("mismatchedDeliveries")
    @DisplayName("delivery의 line/player/item/code/quantity mismatch를 모두 거부한다")
    void rejectsDeliverySnapshotMismatch(
            InventoryRewardDeliveryApi.RewardDeliveryResult mismatch
    ) {
        RewardSettlement settlement = itemSettlement();
        given(settlementReader.getByIdForUpdateOrThrow(100L))
                .willReturn(settlement);
        given(inventoryDeliveryApi.deliverReward(
                1000L, 1L, "IT_ITEM", 2L
        )).willReturn(mismatch);

        assertInconsistent(() -> attempt.process(100L, 1000L));

        assertThat(settlement.getLineByIdOrThrow(1000L).getStatus())
                .isEqualTo(RewardSettlementLineStatus.PENDING);
        verify(settlementWriter, never()).saveAndFlush(settlement);
    }

    @Test
    @DisplayName("EXP Line을 ITEM processor로 호출하면 stable type error다")
    void rejectsExpLine() {
        RewardSettlement settlement = expSettlement();
        given(settlementReader.getByIdForUpdateOrThrow(100L))
                .willReturn(settlement);

        assertError(
                () -> attempt.process(100L, 1000L),
                RewardError.REWARD_SETTLEMENT_LINE_NOT_ITEM
        );
        verify(inventoryDeliveryApi, never()).deliverReward(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    private static Stream<InventoryRewardDeliveryApi.RewardDeliveryResult>
    mismatchedDeliveries() {
        return Stream.of(
                new InventoryRewardDeliveryApi.RewardDeliveryResult(
                        500L, 999L, 1L, 77L, "IT_ITEM", 2L, false
                ),
                new InventoryRewardDeliveryApi.RewardDeliveryResult(
                        500L, 1000L, 2L, 77L, "IT_ITEM", 2L, false
                ),
                new InventoryRewardDeliveryApi.RewardDeliveryResult(
                        500L, 1000L, 1L, 78L, "IT_ITEM", 2L, false
                ),
                new InventoryRewardDeliveryApi.RewardDeliveryResult(
                        500L, 1000L, 1L, 77L, "IT_OTHER", 2L, false
                ),
                new InventoryRewardDeliveryApi.RewardDeliveryResult(
                        500L, 1000L, 1L, 77L, "IT_ITEM", 3L, false
                )
        );
    }

    private InventoryRewardDeliveryApi.RewardDeliveryResult delivery(
            boolean replayed
    ) {
        return new InventoryRewardDeliveryApi.RewardDeliveryResult(
                500L, 1000L, 1L, 77L, "IT_ITEM", 2L, replayed
        );
    }

    private InventoryRewardDeliveryApi.RewardDeliveryReceipt receipt() {
        return new InventoryRewardDeliveryApi.RewardDeliveryReceipt(
                500L, 1000L, 1L, 77L, "IT_ITEM", 2L
        );
    }

    private RewardSettlement itemSettlement() {
        return settlement(RewardDefinition.create(
                "RD_ITEM", "Item", RewardType.ITEM,
                2L, 77L, "IT_ITEM", true
        ));
    }

    private RewardSettlement expSettlement() {
        return settlement(RewardDefinition.create(
                "RD_EXP", "EXP", RewardType.EXP,
                10L, null, null, true
        ));
    }

    private RewardSettlement settlement(RewardDefinition definition) {
        ReflectionTestUtils.setField(definition, "id", 10L);
        RewardProfile profile = RewardProfile.create(
                "RP", "Profile", RewardProfileStatus.ACTIVE
        );
        ReflectionTestUtils.setField(profile, "id", 20L);
        profile.addLine(definition, 0, null);
        RewardSettlement settlement = RewardSettlement.create(
                1L,
                RewardSettlementSourceType.QUEST_COMPLETION,
                30L,
                profile
        );
        ReflectionTestUtils.setField(settlement, "id", 100L);
        ReflectionTestUtils.setField(
                settlement.getLines().getFirst(), "id", 1000L
        );
        return settlement;
    }

    private void assertInconsistent(Runnable action) {
        assertError(
                action,
                RewardError.REWARD_SETTLEMENT_ITEM_DELIVERY_INCONSISTENT
        );
    }

    private void assertError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
