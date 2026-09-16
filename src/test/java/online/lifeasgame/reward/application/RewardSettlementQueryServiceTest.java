package online.lifeasgame.reward.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.reward.application.result.RewardSettlementResult;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardSettlementQueryService")
class RewardSettlementQueryServiceTest {

    @Mock
    private RewardSettlementReader settlementReader;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @Mock
    private RewardSettlement settlement;

    @Mock
    private RewardSettlementLine line;

    private RewardSettlementQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new RewardSettlementQueryService(
                settlementReader,
                currentPlayerAccessor
        );
    }

    @Nested
    @DisplayName("Quest 완료 Settlement를 조회할 때")
    class GetQuestCompletionSettlement {

        @Test
        @DisplayName("현재 Player 소유 Settlement의 저장된 상태와 Line Snapshot만 반환한다")
        void returnsOwnedSettlementSnapshots() {
            Instant createdAt = Instant.parse("2026-09-15T01:00:00Z");
            Instant updatedAt = Instant.parse("2026-09-15T01:01:00Z");
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(42L);
            given(settlementReader.getByIdentityOrThrow(
                    42L,
                    RewardSettlementSourceType.QUEST_COMPLETION,
                    73L
            )).willReturn(settlement);
            given(settlement.getId()).willReturn(91L);
            given(settlement.getSourceType())
                    .willReturn(RewardSettlementSourceType.QUEST_COMPLETION);
            given(settlement.getSourceId()).willReturn(73L);
            given(settlement.getRewardProfileId()).willReturn(101L);
            given(settlement.getRewardProfileCode()).willReturn("RP_ITEM_STARTER");
            given(settlement.getStatus())
                    .willReturn(RewardSettlementStatus.PARTIAL_FAILED);
            given(settlement.getCreatedAt()).willReturn(createdAt);
            given(settlement.getUpdatedAt()).willReturn(updatedAt);
            given(settlement.getLines()).willReturn(List.of(line));
            given(line.getId()).willReturn(92L);
            given(line.getRewardDefinitionId()).willReturn(102L);
            given(line.getRewardDefinitionCode()).willReturn("RD_ITEM_STARTER");
            given(line.getRewardType()).willReturn(RewardType.ITEM);
            given(line.getAmount()).willReturn(2L);
            given(line.getItemId()).willReturn(103L);
            given(line.getItemCode()).willReturn("IT_STARTER");
            given(line.getSortOrder()).willReturn(0);
            given(line.getStatus()).willReturn(RewardSettlementLineStatus.FAILED);
            given(line.getFailureCode())
                    .willReturn(RewardError.REWARD_DEFINITION_NOT_FOUND.code());
            given(line.getCreatedAt()).willReturn(createdAt);
            given(line.getUpdatedAt()).willReturn(updatedAt);

            RewardSettlementResult.Detail result =
                    queryService.getQuestCompletionSettlement(73L);

            assertThat(result.settlementId()).isEqualTo(91L);
            assertThat(result.status())
                    .isEqualTo(RewardSettlementStatus.PARTIAL_FAILED);
            assertThat(result.lines()).singleElement().satisfies(resultLine -> {
                assertThat(resultLine.rewardDefinitionCode())
                        .isEqualTo("RD_ITEM_STARTER");
                assertThat(resultLine.rewardType()).isEqualTo(RewardType.ITEM);
                assertThat(resultLine.amount()).isEqualTo(2L);
                assertThat(resultLine.itemCode()).isEqualTo("IT_STARTER");
                assertThat(resultLine.status())
                        .isEqualTo(RewardSettlementLineStatus.FAILED);
                assertThat(resultLine.failureCode())
                        .isEqualTo(RewardError.REWARD_DEFINITION_NOT_FOUND.code());
            });
            verify(settlementReader).getByIdentityOrThrow(
                    42L,
                    RewardSettlementSourceType.QUEST_COMPLETION,
                    73L
            );
        }

        @Test
        @DisplayName("RP_NONE Settlement는 보상 Line을 만들지 않고 그대로 비어 있다")
        void returnsNoRewardSettlementWithoutInventedLines() {
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(42L);
            given(settlementReader.getByIdentityOrThrow(
                    42L,
                    RewardSettlementSourceType.QUEST_COMPLETION,
                    74L
            )).willReturn(settlement);
            given(settlement.getSourceType())
                    .willReturn(RewardSettlementSourceType.QUEST_COMPLETION);
            given(settlement.getRewardProfileCode()).willReturn("RP_NONE");
            given(settlement.getStatus())
                    .willReturn(RewardSettlementStatus.COMPLETED);
            given(settlement.getLines()).willReturn(List.of());

            RewardSettlementResult.Detail result =
                    queryService.getQuestCompletionSettlement(74L);

            assertThat(result.rewardProfileCode()).isEqualTo("RP_NONE");
            assertThat(result.status()).isEqualTo(RewardSettlementStatus.COMPLETED);
            assertThat(result.lines()).isEmpty();
        }
    }
}
