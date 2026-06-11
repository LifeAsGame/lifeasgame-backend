package online.lifeasgame.social.application.command;


public final class PartyCommand {

    private PartyCommand() {}

    public record Create(
            String name,
            String code,
            String descriptionMd,
            String bannerImageUrl,
            String bannerBgColor,
            String visibility,
            String joinPolicy,
            int maxMembers
    ) {
    }

    public record Rename(String name) {
    }

    public record ChangePolicy(
            String visibility,
            String joinPolicy,
            int maxMembers
    ) {
    }

    public record ChangeDescription(String descriptionMd) {
    }

    public record ChangeEmblem(
            String emblemImageUrl,
            String emblemBgColor
    ) {
    }

    public record TagOp(String tag) {
    }

    public record RequestJoin(String message) {
    }

    public record Approve(Long applicantPlayerId) {
    }

    public record Reject(Long applicantPlayerId) {
    }

    public record TransferLeader(
            Long fromLeaderPlayerId,
            Long toPlayerId
    ) {
    }

    public record Kick(Long targetPlayerId) {
    }

    public record Promote(Long targetPlayerId) {
    }

    public record Demote(Long targetPlayerId) {
    }

    public record Invite(
            Long inviteePlayerId,
            String message,
            String expiresAtIso
    ) {
    }
}
