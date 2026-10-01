package online.lifeasgame.social.application.result;

import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildMember;
import online.lifeasgame.social.domain.GuildWaitMember;

import java.time.Instant;
import java.util.List;

public final class GuildResult {

    public record Summary(
            Long id,
            String name,
            String code,
            String visibility,
            String joinPolicy,
            String status,
            int maxMembers
    ) {
        public static Summary from(Guild guild) {
            return new Summary(
                    guild.getId(),
                    guild.getName().getOriginal(),
                    guild.getCode().getValue(),
                    guild.getVisibility().name(),
                    guild.getJoinPolicy().name(),
                    guild.getStatus().name(),
                    guild.getMaxMembers()
            );
        }
    }

    public record Info(
            Long id,
            Long playerId,
            String name,
            String code,
            String visibility,
            String joinPolicy,
            String status,
            int maxMembers,
            List<String> tags,
            String descriptionMd,
            String emblemImageUrl,
            String emblemBgColor,
            Long leaderPlayerId,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Info from(Guild guild) {
            return new Info(
                    guild.getId(),
                    guild.getPlayerId(),
                    guild.getName().getOriginal(),
                    guild.getCode().getValue(),
                    guild.getVisibility().name(),
                    guild.getJoinPolicy().name(),
                    guild.getStatus().name(),
                    guild.getMaxMembers(),
                    guild.getTags().stream().toList(),
                    guild.getDescription() == null ? null : guild.getDescription().getMd(),
                    guild.getEmblem() == null ? null : guild.getEmblem().getImageUrl(),
                    guild.getEmblem() == null ? null : guild.getEmblem().getBgColor(),
                    guild.getLeaderPlayerId(),
                    guild.getCreatedAt(),
                    guild.getUpdatedAt()
            );
        }
    }
    
    public record MyGuild(Long id, String name, String code, String status, String myRole, int memberCount, int maxMembers) {
        public static MyGuild from(Guild group, Long playerId) {
            return new MyGuild(group.getId(), group.getName().getOriginal(), group.getCode().getValue(),
                    group.getStatus().name(), group.findMember(playerId).orElseThrow().getRole().name(),
                    group.memberCount(), group.getMaxMembers());
        }
    }

    public record Member(Long playerId, String role, String joinedAt) {
        public static Member from(GuildMember member) {
            return new Member(member.getPlayerId(), member.getRole().name(), member.getJoinedAt().toString());
        }
    }

    public record Pending(Long id, Long guildId, String name, String code, Long playerId, String type, String status,
                          String message, String requestedAt, String expiresAt) {
        public static Pending from(GuildWaitMember wait) {
            Guild group = wait.getGuild();
            return new Pending(wait.getId(), group.getId(), group.getName().getOriginal(), group.getCode().getValue(),
                    wait.getPlayerId(), wait.getType().name(), wait.getStatus().name(), wait.getMessage(),
                    wait.getRequestedAt().toString(), wait.getExpiresAt() == null ? null : wait.getExpiresAt().toString());
        }
    }

    public record Me(String myRole, boolean pendingJoin, boolean pendingInvitation, List<String> actions) {}

    public record Page<T>(
            List<T> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static <T> Page<T> of(
                List<T> contents,
                int page,
                int size,
                long totalElements
        ) {
            int totalPages = (int) Math.ceil(totalElements / (double) Math.max(size, 1));
            return new Page<>(contents, page, size, totalElements, totalPages);
        }
    }
}
