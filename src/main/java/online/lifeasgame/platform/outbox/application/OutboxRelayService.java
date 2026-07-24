package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxLeaseRecoveryService leaseRecoveryService;
    private final OutboxClaimService claimService;
    private final OutboxDispatchAttempt dispatchAttempt;
    private final OutboxCompletionService completionService;
    private final OutboxFailureService failureService;

    public OutboxRelayResult relayBatch() {
        int recovered = leaseRecoveryService.recoverStale();
        List<OutboxClaim> claims = claimService.claimBatch();
        int published = 0;
        int failed = 0;

        for (OutboxClaim claim : claims) {
            try {
                dispatchAttempt.dispatch(claim);
            } catch (RuntimeException exception) {
                failureService.recordFailure(claim, exception);
                failed++;
                continue;
            }

            completionService.complete(claim);
            published++;
        }

        return new OutboxRelayResult(
                recovered,
                claims.size(),
                published,
                failed
        );
    }
}
