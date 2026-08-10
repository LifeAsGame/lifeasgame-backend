package online.lifeasgame.quest.api.player.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class QuestRouteRequest {

    private QuestRouteRequest() {
    }

    public record Advance(
            @NotNull @Positive Long expectedStepId
    ) {
    }
}
