package online.lifeasgame.social.api.player.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class RolePartyRequest {
    private RolePartyRequest() {}

    public record Details(@NotBlank @Size(max = 120) String name,
                          @Size(max = 1000) String description,
                          @Min(2) @Max(50) int maxMembers) {}
    public record Invite(@NotNull @Positive Long inviteePlayerId) {}
    public record Transfer(@NotNull @Positive Long toPlayerId) {}
}
