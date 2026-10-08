package online.lifeasgame.demo.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.demo.domain.DemoError;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoActorScope implements DemoActorScopeApi {
    private final DemoRunStore store;
    private final DemoProperties properties;

    @Override
    public boolean sameBoundary(Long actorPlayerId, Long targetPlayerId) {
        return store.sameBoundary(actorPlayerId, targetPlayerId);
    }

    @Override
    public void requireSameBoundary(Long actorPlayerId, Long targetPlayerId) {
        if (!sameBoundary(actorPlayerId, targetPlayerId)) throw new DomainException(DemoError.ACTOR_FORBIDDEN);
    }

    @Override
    public void requireActive(Long userId, Long playerId) {
        DemoRunStore.Actor actor = store.actorByUser(userId);
        if (actor == null) return;
        if (!properties.enabled() || playerId == null || !playerId.equals(actor.playerId())) {
            throw new DomainException(DemoError.ACTOR_FORBIDDEN);
        }
        DemoRunStore.Run run = store.byId(actor.runId());
        if (run.status().equals("CLOSED") || run.status().equals("EXPIRED")) {
            throw new DomainException(DemoError.RUN_EXPIRED);
        }
        if (!run.status().equals("READY")) throw new DomainException(DemoError.NOT_READY);
    }
}
