package online.lifeasgame.reward.application;

import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.result.RewardSettlementExpProcessResult;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardSettlementExpProcessService")
class RewardSettlementExpProcessServiceTest {

    @Mock
    private RewardSettlementExpProcessAttempt processAttempt;

    @Mock
    private RewardSettlementLineFailureRecorder failureRecorder;

    @Mock
    private RewardSettlementExpReplayRecovery replayRecovery;

    private RewardSettlementExpProcessService service;

    @BeforeEach
    void setUp() {
        service = new RewardSettlementExpProcessService(processAttempt, failureRecorder, replayRecovery);
    }

    @Nested
    @DisplayName("성공 attempt 결과를 처리할 때")
    class HandleSuccess {

        @Test
        @DisplayName("성공 결과를 그대로 반환하고 실패 기록은 호출하지 않는다")
        void returnsSuccess() {
            RewardSettlementExpProcessResult expected = new RewardSettlementExpProcessResult(
                    1L, 10L, 100L,
                    RewardSettlementLineStatus.SUCCEEDED, RewardSettlementStatus.COMPLETED,
                    10L, 10L, 0L, 1, 1, 0L, 10L, false, 50L
            );
            given(processAttempt.process(1L, 10L)).willReturn(expected);

            RewardSettlementExpProcessResult result = service.process(1L, 10L);

            assertThat(result).isEqualTo(expected);
            verify(failureRecorder, never()).record(1L, 10L, PlayerError.PLAYER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("성공 attempt가 실패할 때")
    class HandleFailure {

        @Test
        @DisplayName("알려진 DomainException은 rollback 이후 별도 실패 기록을 남기고 재전파한다")
        void recordsKnownDomainFailure() {
            DomainException exception = new DomainException(PlayerError.PLAYER_NOT_FOUND);
            given(processAttempt.process(1L, 10L)).willThrow(exception);

            assertThatThrownBy(() -> service.process(1L, 10L)).isSameAs(exception);
            verify(failureRecorder).record(1L, 10L, PlayerError.PLAYER_NOT_FOUND);
        }

        @Test
        @DisplayName("예상하지 못한 RuntimeException은 실패로 확정하지 않고 그대로 전파한다")
        void propagatesUnexpectedFailureWithoutRecording() {
            RuntimeException exception = new RuntimeException("unexpected");
            given(processAttempt.process(1L, 10L)).willThrow(exception);

            assertThatThrownBy(() -> service.process(1L, 10L)).isSameAs(exception);
            verify(failureRecorder, never()).record(1L, 10L, PlayerError.PLAYER_NOT_FOUND);
        }

        @Test
        @DisplayName("GrowthChange unique 충돌은 rollback 후 fresh transaction replay로 복구한다")
        void recoversUniqueConflictWithFreshReplay() {
            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("duplicate reward line");
            RewardSettlementExpProcessResult replay = new RewardSettlementExpProcessResult(
                    1L, 10L, 100L,
                    RewardSettlementLineStatus.SUCCEEDED, RewardSettlementStatus.COMPLETED,
                    10L, 10L, 0L, 1, 1, 0L, 10L, true, 50L
            );
            given(processAttempt.process(1L, 10L)).willThrow(exception);
            given(replayRecovery.findCompletedReplay(1L, 10L)).willReturn(Optional.of(replay));

            RewardSettlementExpProcessResult result = service.process(1L, 10L);

            assertThat(result).isEqualTo(replay);
            verify(failureRecorder, never()).record(1L, 10L, PlayerError.PLAYER_NOT_FOUND);
        }
    }
}
