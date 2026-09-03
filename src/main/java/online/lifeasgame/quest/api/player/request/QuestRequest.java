package online.lifeasgame.quest.api.player.request;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

public final class QuestRequest {

    private QuestRequest() {
    }

    public record Accept(
            @Null Long partyId,
            @Null Long guildId
    ) {
    }

    public record Cancel(
            @Size(max = 200) String reason
    ) {
    }

    public record ClaimReward(
            @Size(max = 120) String idempotencyKey
    ) {
    }
}
