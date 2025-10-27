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
        public static Summary from(Guild g) {
            return new Summary(
                    g.getId(),
                    g.getName().getOriginal(),
                    g.getCode().getValue(),
                    g.getVisibility().name(),
                    g.getJoinPolicy().name(),
                    g.getStatus().name(),
                    g.getMaxMembers()
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
        public static Info from(Guild g) {
            return new Info(
                    g.getId(),
                    g.getPlayerId(),
                    g.getName().getOriginal(),
                    g.getCode().getValue(),
                    g.getVisibility().name(),
                    g.getJoinPolicy().name(),
                    g.getStatus().name(),
                    g.getMaxMembers(),
                    g.getTags().stream().toList(),
                    g.getDescription() == null ? null : g.getDescription().getMd(),
                    g.getEmblem() == null ? null : g.getEmblem().getImageUrl(),
                    g.getEmblem() == null ? null : g.getEmblem().getBgColor(),
                    g.getLeaderPlayerId(),
                    g.getCreatedAt(),
                    g.getUpdatedAt()
            );
        }
    }
    
    public record Page<T>(List<T> contents, int page, int size, long totalElements, int totalPages) {
        public static <T> Page<T> of(List<T> contents, int page, int size, long totalElements) {
            int totalPages = (int) Math.ceil(totalElements / (double) Math.max(size, 1));
            return new Page<>(contents, page, size, totalElements, totalPages);
        }
    }
}
