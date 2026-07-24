package online.lifeasgame.lifelog.quick.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuickRecordFacade {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final QuickRecordService quickRecordService;

    public QuickRecordResult.Recorded record(
            String idempotencyKey,
            QuickRecordCommand.Create command
    ) {
        return quickRecordService.record(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                idempotencyKey,
                command
        );
    }
}
