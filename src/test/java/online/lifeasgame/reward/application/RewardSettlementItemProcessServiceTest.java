package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardSettlementItemProcessService")
class RewardSettlementItemProcessServiceTest {

    @Mock
    private RewardSettlementItemProcessAttempt processAttempt;

    @Mock
    private RewardSettlementLineFailureRecorder failureRecorder;

    private RewardSettlementItemProcessService service;

    @BeforeEach
    void setUp() {
        service = new RewardSettlementItemProcessService(
                processAttempt, failureRecorder
        );
    }

    @Test
    @DisplayName("예상 가능한 Domain failure는 별도 Transaction으로 기록하고 재전파한다")
    void recordsDomainFailure() {
        DomainException failure = new DomainException(
                InventoryError.MAILBOX_FULL
        );
        given(processAttempt.process(1L, 2L)).willThrow(failure);

        assertThatThrownBy(() -> service.process(1L, 2L))
                .isSameAs(failure);
        verify(failureRecorder).record(
                1L, 2L, InventoryError.MAILBOX_FULL
        );
    }

    @Test
    @DisplayName("system failure는 Line을 확정 실패시키지 않고 그대로 전파한다")
    void propagatesSystemFailure() {
        RuntimeException failure = new RuntimeException("system");
        given(processAttempt.process(1L, 2L)).willThrow(failure);

        assertThatThrownBy(() -> service.process(1L, 2L))
                .isSameAs(failure);
        verify(failureRecorder, never()).record(
                1L, 2L, InventoryError.MAILBOX_FULL
        );
    }
}
