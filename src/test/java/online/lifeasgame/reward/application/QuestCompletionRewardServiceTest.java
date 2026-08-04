package online.lifeasgame.reward.application;

import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.reward.application.event.QuestRewardReadyFact;
import online.lifeasgame.reward.domain.*;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("QuestCompletionRewardService")
class QuestCompletionRewardServiceTest {

    private RewardSettlementCreateService createService;
    private RewardSettlementReader settlementReader;
    private RewardSettlementExpProcessService expProcessService;
    private RewardSettlementItemProcessService itemProcessService;
    private QuestCompletionRewardService service;

    @BeforeEach
    void setUp() {
        createService = mock(RewardSettlementCreateService.class);
        settlementReader = mock(RewardSettlementReader.class);
        expProcessService =
                mock(RewardSettlementExpProcessService.class);
        itemProcessService = mock(RewardSettlementItemProcessService.class);
        service = new QuestCompletionRewardService(
                createService,
                settlementReader,
                expProcessService,
                itemProcessService
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = RewardSettlementLineStatus.class,
            names = {"PENDING", "SUCCEEDED"}
    )
    @DisplayName("PENDING과 SUCCEEDED EXP는 Processor에서 성공/replay를 검증한다")
    void processesPendingAndSucceededExp(
            RewardSettlementLineStatus status
    ) {
        RewardSettlementLine exp = line(
                701L,
                RewardType.EXP,
                status,
                null
        );
        RewardSettlementLine item = line(
                702L,
                RewardType.ITEM,
                RewardSettlementLineStatus.PENDING,
                null
        );
        stubCreated(
                settlement(
                        700L,
                        "RP_EXP_AND_ITEM_FIRST_STEP_20",
                        List.of(exp, item)
                )
        );

        service.process(fact("RP_EXP_AND_ITEM_FIRST_STEP_20"));

        verify(expProcessService).process(700L, 701L);
        verify(expProcessService, never()).process(700L, 702L);
        verify(itemProcessService).process(700L, 702L);
        verifyNoInteractions(settlementReader);
    }

    @Test
    @DisplayName("RP_NONE의 빈 Settlement는 EXP Processor를 호출하지 않는다")
    void leavesNoRewardSettlementCompleted() {
        stubCreated(settlement(710L, "RP_NONE", List.of()));

        service.process(fact("RP_NONE"));

        verifyNoInteractions(
                settlementReader, expProcessService, itemProcessService
        );
    }

    @Test
    @DisplayName("기존 FAILED EXP는 Processor와 RetryPreparation 없이 skip한다")
    void skipsPreviouslyFailedExp() {
        RewardSettlementLine failed = line(
                711L,
                RewardType.EXP,
                RewardSettlementLineStatus.FAILED,
                PlayerError.PLAYER_NOT_FOUND.code()
        );
        stubCreated(settlement(
                710L,
                "RP_EXP_30",
                List.of(failed)
        ));

        service.process(fact("RP_EXP_30"));

        verifyNoInteractions(
                settlementReader, expProcessService, itemProcessService
        );
    }

    @Test
    @DisplayName("DomainException 후 fresh FAILED만 소비하고 다음 EXP Line을 처리한다")
    void consumesOnlyFreshDurableFailureAndContinues() {
        RewardSettlementLine first = line(
                721L,
                RewardType.EXP,
                RewardSettlementLineStatus.PENDING,
                null
        );
        RewardSettlementLine second = line(
                722L,
                RewardType.EXP,
                RewardSettlementLineStatus.PENDING,
                null
        );
        stubCreated(settlement(
                720L,
                "RP_EXP_30",
                List.of(first, second)
        ));
        doThrow(new DomainException(PlayerError.PLAYER_NOT_FOUND))
                .when(expProcessService).process(720L, 721L);
        RewardSettlement fresh = mock(RewardSettlement.class);
        RewardSettlementLine durableFailed = line(
                721L,
                RewardType.EXP,
                RewardSettlementLineStatus.FAILED,
                PlayerError.PLAYER_NOT_FOUND.code()
        );
        when(fresh.getLineByIdOrThrow(721L))
                .thenReturn(durableFailed);
        when(settlementReader.getByIdInNewTransactionOrThrow(720L))
                .thenReturn(fresh);

        service.process(fact("RP_EXP_30"));

        verify(settlementReader)
                .getByIdInNewTransactionOrThrow(720L);
        verify(expProcessService).process(720L, 722L);
    }

    @Test
    @DisplayName("ITEM DomainException 후 fresh FAILED를 확인하면 Event 처리를 완료한다")
    void consumesDurableItemFailure() {
        RewardSettlementLine item = line(
                726L,
                RewardType.ITEM,
                RewardSettlementLineStatus.PENDING,
                null
        );
        stubCreated(settlement(
                725L,
                "RP_EXP_AND_ITEM_FIRST_STEP_20",
                List.of(item)
        ));
        doThrow(new DomainException(InventoryError.MAILBOX_FULL))
                .when(itemProcessService).process(725L, 726L);
        RewardSettlement fresh = mock(RewardSettlement.class);
        RewardSettlementLine failed = line(
                726L,
                RewardType.ITEM,
                RewardSettlementLineStatus.FAILED,
                InventoryError.MAILBOX_FULL.code()
        );
        when(fresh.getLineByIdOrThrow(726L)).thenReturn(failed);
        when(settlementReader.getByIdInNewTransactionOrThrow(725L))
                .thenReturn(fresh);

        service.process(fact("RP_EXP_AND_ITEM_FIRST_STEP_20"));

        verify(settlementReader)
                .getByIdInNewTransactionOrThrow(725L);
        verify(itemProcessService).process(725L, 726L);
    }

    @Test
    @DisplayName("ITEM system failure는 fresh 조회 없이 Outbox retry로 전파한다")
    void propagatesUnexpectedItemFailure() {
        RewardSettlementLine item = line(
                756L,
                RewardType.ITEM,
                RewardSettlementLineStatus.PENDING,
                null
        );
        stubCreated(settlement(
                755L,
                "RP_EXP_AND_ITEM_FIRST_STEP_20",
                List.of(item)
        ));
        doThrow(new IllegalStateException("item system"))
                .when(itemProcessService).process(755L, 756L);

        assertThatThrownBy(() -> service.process(fact(
                "RP_EXP_AND_ITEM_FIRST_STEP_20"
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessage("item system");
        verifyNoInteractions(settlementReader);
    }

    @ParameterizedTest
    @EnumSource(
            value = RewardSettlementLineStatus.class,
            names = {"PENDING", "SUCCEEDED"}
    )
    @DisplayName("DomainException 후 fresh Line이 FAILED가 아니면 원래 예외를 전파한다")
    void propagatesWhenFreshLineIsNotFailed(
            RewardSettlementLineStatus freshStatus
    ) {
        RewardSettlementLine line = line(
                731L,
                RewardType.EXP,
                RewardSettlementLineStatus.PENDING,
                null
        );
        stubCreated(settlement(
                730L,
                "RP_EXP_30",
                List.of(line)
        ));
        DomainException original =
                new DomainException(PlayerError.PLAYER_NOT_FOUND);
        doThrow(original).when(expProcessService).process(730L, 731L);
        RewardSettlement fresh = mock(RewardSettlement.class);
        RewardSettlementLine freshLine = line(
                731L,
                RewardType.EXP,
                freshStatus,
                null
        );
        when(fresh.getLineByIdOrThrow(731L)).thenReturn(freshLine);
        when(settlementReader.getByIdInNewTransactionOrThrow(730L))
                .thenReturn(fresh);

        assertThatThrownBy(() -> service.process(fact("RP_EXP_30")))
                .isSameAs(original);
    }

    @Test
    @DisplayName("fresh Settlement 또는 Line 조회 실패는 해당 stable 오류를 전파한다")
    void propagatesFreshLookupFailure() {
        RewardSettlementLine line = line(
                741L,
                RewardType.EXP,
                RewardSettlementLineStatus.PENDING,
                null
        );
        stubCreated(settlement(
                740L,
                "RP_EXP_30",
                List.of(line)
        ));
        doThrow(new DomainException(PlayerError.PLAYER_NOT_FOUND))
                .when(expProcessService).process(740L, 741L);
        DomainException settlementMissing = new DomainException(
                RewardError.REWARD_SETTLEMENT_NOT_FOUND
        );
        when(settlementReader.getByIdInNewTransactionOrThrow(740L))
                .thenThrow(settlementMissing);

        assertThatThrownBy(() -> service.process(fact("RP_EXP_30")))
                .isSameAs(settlementMissing);

        RewardSettlement fresh = mock(RewardSettlement.class);
        DomainException lineMissing = new DomainException(
                RewardError.REWARD_SETTLEMENT_LINE_NOT_FOUND
        );
        when(fresh.getLineByIdOrThrow(741L)).thenThrow(lineMissing);
        doReturn(fresh).when(settlementReader)
                .getByIdInNewTransactionOrThrow(740L);

        assertThatThrownBy(() -> service.process(fact("RP_EXP_30")))
                .isSameAs(lineMissing);
    }

    @Test
    @DisplayName("unexpected RuntimeException은 fresh 조회 없이 Outbox retry로 전파한다")
    void propagatesUnexpectedRuntimeException() {
        RewardSettlementLine line = line(
                751L,
                RewardType.EXP,
                RewardSettlementLineStatus.PENDING,
                null
        );
        stubCreated(settlement(
                750L,
                "RP_EXP_30",
                List.of(line)
        ));
        doThrow(new IllegalStateException("unexpected"))
                .when(expProcessService).process(750L, 751L);

        assertThatThrownBy(() -> service.process(fact("RP_EXP_30")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unexpected");
        verifyNoInteractions(settlementReader);
    }

    @Test
    @DisplayName("동일 Settlement identity의 다른 Event profile은 stable 409로 거부한다")
    void rejectsConflictingProfileSnapshot() {
        when(createService.create(
                anyLong(),
                any(RewardSettlementSourceType.class),
                anyLong(),
                anyString()
        )).thenThrow(new DomainException(
                RewardError.REWARD_SETTLEMENT_SOURCE_PROFILE_CONFLICT
        ));

        assertThatThrownBy(() -> service.process(fact("RP_NONE")))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        RewardError
                                                .REWARD_SETTLEMENT_SOURCE_PROFILE_CONFLICT
                                )
                );
        verifyNoInteractions(
                settlementReader, expProcessService, itemProcessService
        );
    }

    private void stubCreated(RewardSettlement settlement) {
        when(createService.create(
                anyLong(),
                any(RewardSettlementSourceType.class),
                anyLong(),
                anyString()
        )).thenReturn(settlement);
    }

    private RewardSettlement settlement(
            Long id,
            String profileCode,
            List<RewardSettlementLine> lines
    ) {
        RewardSettlement settlement = mock(RewardSettlement.class);
        when(settlement.getId()).thenReturn(id);
        when(settlement.getRewardProfileCode()).thenReturn(profileCode);
        when(settlement.getLines()).thenReturn(lines);
        return settlement;
    }

    private RewardSettlementLine line(
            Long id,
            RewardType type,
            RewardSettlementLineStatus status,
            String failureCode
    ) {
        RewardSettlementLine line = mock(RewardSettlementLine.class);
        when(line.getId()).thenReturn(id);
        when(line.getRewardType()).thenReturn(type);
        when(line.getStatus()).thenReturn(status);
        when(line.getFailureCode()).thenReturn(failureCode);
        return line;
    }

    private QuestRewardReadyFact fact(String profileCode) {
        return new QuestRewardReadyFact(
                2190L,
                21900L,
                profileCode,
                219L,
                "Q_FIRST_STEP",
                7,
                Instant.parse("2026-07-30T03:00:01Z"),
                "quest:219:completed:reward"
        );
    }
}
