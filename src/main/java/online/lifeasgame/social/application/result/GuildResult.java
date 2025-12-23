package online.lifeasgame.social.application.result;

import online.lifeasgame.social.domain.Guild;

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
