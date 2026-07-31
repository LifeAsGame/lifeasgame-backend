package online.lifeasgame.reward.application;

import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.event.QuestRewardReadyFact;
import online.lifeasgame.reward.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("QuestCompletionRewardService")
class QuestCompletionRewardServiceTest {

    private RewardSettlementCreateService createService;
    private RewardSettlementExpProcessService expProcessService;
    private QuestCompletionRewardService service;

    @BeforeEach
    void setUp() {
        createService = mock(RewardSettlementCreateService.class);
        expProcessService =
                mock(RewardSettlementExpProcessService.class);
        service = new QuestCompletionRewardService(
                createService,
                expProcessService
        );
    }

    @Test
    @DisplayName("acceptanceId와 Event profile로 Settlement를 만들고 EXP만 처리한다")
    void createsSettlementAndProcessesOnlyExp() {
        RewardSettlement created = mock(RewardSettlement.class);
        RewardSettlementLine exp = line(701L, RewardType.EXP);
        RewardSettlementLine item = line(702L, RewardType.ITEM);
        when(created.getId()).thenReturn(700L);
        when(created.getLines()).thenReturn(List.of(exp, item));
        when(createService.create(
                2190L,
                RewardSettlementSourceType.QUEST_COMPLETION,
                21900L,
                "RP_EXP_AND_ITEM_FIRST_STEP_20"
        )).thenReturn(created);

        service.process(fact("RP_EXP_AND_ITEM_FIRST_STEP_20"));

        verify(expProcessService).process(700L, 701L);
        verify(expProcessService, never()).process(700L, 702L);
    }

    @Test
    @DisplayName("RP_NONE의 빈 Settlement는 EXP Processor를 호출하지 않는다")
    void leavesNoRewardSettlementCompleted() {
        RewardSettlement created = mock(RewardSettlement.class);
        when(created.getId()).thenReturn(710L);
        when(created.getLines()).thenReturn(List.of());
        when(createService.create(
                2190L,
                RewardSettlementSourceType.QUEST_COMPLETION,
                21900L,
                "RP_NONE"
        )).thenReturn(created);

        service.process(fact("RP_NONE"));

        verifyNoInteractions(expProcessService);
    }

    @Test
    @DisplayName("EXP known DomainException은 소비하고 unexpected RuntimeException은 전파한다")
    void handlesOnlyKnownExpFailure() {
        RewardSettlement created = mock(RewardSettlement.class);
        RewardSettlementLine first = line(721L, RewardType.EXP);
        RewardSettlementLine second = line(722L, RewardType.EXP);
        when(created.getId()).thenReturn(720L);
        when(created.getLines()).thenReturn(List.of(first, second));
        when(createService.create(anyLong(), any(), anyLong(), anyString()))
                .thenReturn(created);
        doThrow(new DomainException(PlayerError.PLAYER_NOT_FOUND))
                .when(expProcessService).process(720L, 721L);

        service.process(fact("RP_EXP_30"));

        verify(expProcessService).process(720L, 722L);

        doThrow(new IllegalStateException("unexpected"))
                .when(expProcessService).process(720L, 722L);
        assertThatThrownBy(() -> service.process(fact("RP_EXP_30")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unexpected");
    }

    private RewardSettlementLine line(Long id, RewardType type) {
        RewardSettlementLine line = mock(RewardSettlementLine.class);
        when(line.getId()).thenReturn(id);
        when(line.getRewardType()).thenReturn(type);
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
