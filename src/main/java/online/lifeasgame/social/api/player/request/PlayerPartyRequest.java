package online.lifeasgame.social.api.player.request;

import jakarta.validation.constraints.*;

public final class PlayerPartyRequest {
    public record Create(
            @NotBlank String name,
            @NotBlank String code,
            String descriptionMd,
            String bannerImageUrl,
            String bannerBgColor,
            @Pattern(regexp = "PUBLIC|PRIVATE") String visibility,
            @Pattern(regexp = "OPEN|APPROVAL|INVITE_ONLY") String joinPolicy,
            @Min(1) @Max(500) int maxMembers
    ) {
    }

    public record Rename(@NotBlank String name) {
    }

    public record ChangePolicy(
            @Pattern(regexp = "PUBLIC|PRIVATE") String visibility,
            @Pattern(regexp = "OPEN|APPROVAL|INVITE_ONLY") String joinPolicy,
            @Min(1) @Max(500) int maxMembers
    ) {
    }

    public record ChangeDescription(String descriptionMd) {
    }

    public record ChangeBanner(
            String bannerImageUrl,
            String bannerBgColor
    ) {
    }

    public record TagOp(
            @NotBlank @Size(max = 64) String tag
    ) {
    }

    public record RequestJoin(
            @Size(max = 1000) String message
    ) {
    }

    public record Approve(
            @NotNull Long applicantPlayerId
    ) {
    }

    public record Reject(
            @NotNull Long applicantPlayerId
    ) {
    }

    public record TransferLeader(
            @NotNull Long fromLeaderPlayerId,
            @NotNull Long toPlayerId
    ) {
    }

    public record Kick(
            @NotNull Long targetPlayerId
    ) {
    }

    public record MemberOp(
            @NotNull Long targetPlayerId
    ) {
    }

    public record Invite(
            @NotNull Long inviteePlayerId,
            @Size(max = 1000) String message,
            String expiresAt
    ) {
    }
}
