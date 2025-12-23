package online.lifeasgame.social.api.player.request;

import jakarta.validation.constraints.*;

public final class PlayerGuildRequest {

    private PlayerGuildRequest() {}

    public record Create(
            @NotBlank String name,
            @NotBlank String code,
            String descriptionMd,
            String emblemImageUrl,
            String emblemBgColor,
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

    public record ChangeEmblem(
            String emblemImageUrl,
            String emblemBgColor
    ) {
    }

    public record TagOp(
            @NotBlank String tag
    ) {
    }

    public record RequestJoin(String message) {
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
            String message,
            String expiresAt
    ) {
    }
}
