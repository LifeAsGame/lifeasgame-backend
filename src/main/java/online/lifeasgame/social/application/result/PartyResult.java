package online.lifeasgame.social.application.result;

import online.lifeasgame.social.domain.Party;

import java.time.Instant;
import java.util.List;

public final class PartyResult {

    public record Summary(
            Long id,
            String name,
            String code,
            String visibility,
            String joinPolicy,
            String status,
            int maxMembers
    ) {
        public static Summary from(Party g) {
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
            String bannerImageUrl,
            String bannerBgColor,
            Long leaderPlayerId,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Info from(Party g) {
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
                    g.getBanner() == null ? null : g.getBanner().getImageUrl(),
                    g.getBanner() == null ? null : g.getBanner().getBgColor(),
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
