package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.domain.event.UserRegistered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserOnBoardingPolicy {

    private final UserSettingService userSettingService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(UserRegistered e) {
        userSettingService.ensureDefaultIfMissing(e.userId());
    }
}
