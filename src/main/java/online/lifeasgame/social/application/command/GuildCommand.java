package online.lifeasgame.social.application.command;


public final class GuildCommand {
    public record Create(
            String name,
            String code,
            String descriptionMd,
            String emblemImageUrl,
            String emblemBgColor,
            String visibility,
            String joinPolicy,
            int maxMembers
    ) {
        public static Create of(
                String name,
                String code,
                String descriptionMd,
                String emblemImageUrl,
                String emblemBgColor,
                String visibility,
                String joinPolicy,
                int maxMembers
        ) {
            return new Create(
                    name,
                    code,
                    descriptionMd,
                    emblemImageUrl,
                    emblemBgColor,
                    visibility,
                    joinPolicy,
                    maxMembers
            );
        }
    }

    public record Rename(String name) {
        public static Rename of(String name) {
            return new Rename(name);
        }
    }

    public record ChangePolicy(String visibility, String joinPolicy, int maxMembers) {
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

    public record TagOp(String tag) {
        public static TagOp of(String t) {
            return new TagOp(t);
        }
    }

    public record RequestJoin(String message) {
        public static RequestJoin of(String m) {
            return new RequestJoin(m);
        }
    }

    public record Approve(Long applicantPlayerId) {
        public static Approve of(Long id) {
            return new Approve(id);
        }
    }

    public record Reject(Long applicantPlayerId) {
        public static Reject of(Long id) {
            return new Reject(id);
        }
    }

    public record TransferLeader(Long fromLeaderPlayerId, Long toPlayerId) {
        public static TransferLeader of(Long f, Long t) {
            return new TransferLeader(f, t);
        }
    }

    public record Kick(Long targetPlayerId) {
        public static Kick of(Long id) {
            return new Kick(id);
        }
    }

    public record Promote(Long targetPlayerId) {
        public static Promote of(Long id) {
            return new Promote(id);
        }
    }

    public record Demote(Long targetPlayerId) {
        public static Demote of(Long id) {
            return new Demote(id);
        }
    }

    public record Invite(Long inviteePlayerId, String message, String expiresAtIso) {
        public static Invite of(Long id, String m, String at) {
            return new Invite(id, m, at);
        }
    }
}
