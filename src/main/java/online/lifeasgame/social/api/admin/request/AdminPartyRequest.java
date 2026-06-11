package online.lifeasgame.social.api.admin.request;

import jakarta.validation.constraints.*;


public final class AdminPartyRequest {

    private AdminPartyRequest() {}

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

    public record Rename(
            @NotBlank String name
    ) {
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

    public record TagOp(@NotBlank @Size(max = 64) String tag) {
    }

    public record Approve(@NotNull Long applicantPlayerId) {
    }

    public record Reject(@NotNull Long applicantPlayerId) {
    }

    public record Kick(@NotNull Long targetPlayerId) {
    }

    public record TransferLeader(
            @NotNull Long fromLeaderPlayerId,
            @NotNull Long toPlayerId
    ) {
    }

    public record Invite(
            @NotNull Long inviteePlayerId,
            @Size(max = 1000) String message,
            String expiresAt
    ) {
    }

    public record MemberOp(
            @NotNull Long targetPlayerId
    ) {
    }

    public record RequestJoin(
            @Size(max = 1000) String message
    ) {
    }
}
