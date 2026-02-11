package online.lifeasgame.quest.api.player.request;

import jakarta.validation.constraints.Size;

public final class QuestRequest {

    private QuestRequest() {
    }

    public record Accept(
            Long partyId,
            Long guildId,
            @Size(max = 120) String idempotencyKey
    ) {
    }

    public record Cancel(
            @Size(max = 200) String reason,
            @Size(max = 120) String idempotencyKey
    ) {
    }

    public record ClaimReward(
            @Size(max = 120) String idempotencyKey
    ) {
    }
}
