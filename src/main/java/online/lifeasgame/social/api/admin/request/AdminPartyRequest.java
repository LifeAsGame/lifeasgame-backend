package online.lifeasgame.social.api.admin.request;

import jakarta.validation.constraints.*;


public final class AdminPartyRequest {
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
        public static Create of(String n, String c, String d, String u, String bg, String v, String j, int m) {
            return new Create(n, c, d, u, bg, v, j, m);
        }
    }

    public record Rename(@NotBlank String name) {
        public static Rename of(String n) {
            return new Rename(n);
        }
    }

    public record ChangePolicy(
            @Pattern(regexp = "PUBLIC|PRIVATE") String visibility,
            @Pattern(regexp = "OPEN|APPROVAL|INVITE_ONLY") String joinPolicy,
            @Min(1) @Max(500) int maxMembers
    ) {
        public static ChangePolicy of(String v, String j, int m) {
            return new ChangePolicy(v, j, m);
        }
    }

    public record ChangeDescription(String descriptionMd) {
        public static ChangeDescription of(String d) {
            return new ChangeDescription(d);
        }
    }

    public record ChangeEmblem(String emblemImageUrl, String emblemBgColor) {
        public static ChangeEmblem of(String u, String bg) {
            return new ChangeEmblem(u, bg);
        }
    }

    public record TagOp(@NotBlank @Size(max = 64) String tag) {
        public static TagOp of(String t) {
            return new TagOp(t);
        }
    }

    public record Approve(@NotNull Long applicantPlayerId) {
        public static Approve of(Long id) {
            return new Approve(id);
        }
    }

    public record Reject(@NotNull Long applicantPlayerId) {
        public static Reject of(Long id) {
            return new Reject(id);
        }
    }

    public record Kick(@NotNull Long targetPlayerId) {
        public static Kick of(Long id) {
            return new Kick(id);
        }
    }

    public record TransferLeader(@NotNull Long fromLeaderPlayerId, @NotNull Long toPlayerId) {
        public static TransferLeader of(Long f, Long t) {
            return new TransferLeader(f, t);
        }
    }

    public record Invite(@NotNull Long inviteePlayerId, @Size(max = 1000) String message, String expiresAt) {
        public static Invite of(Long id, String m, String at) {
            return new Invite(id, m, at);
        }
    }

    public record MemberOp(@NotNull Long targetPlayerId) {
        public static MemberOp of(Long id) {
            return new MemberOp(id);
        }
    }

    public record RequestJoin(@Size(max = 1000) String message) {
        public static RequestJoin of(String m) {
            return new RequestJoin(m);
        }
    }
}
