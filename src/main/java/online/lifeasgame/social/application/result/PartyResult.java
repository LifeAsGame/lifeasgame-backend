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
        public static Summary from(Party party) {
            return new Summary(
                    party.getId(),
                    party.getName().getOriginal(),
                    party.getCode().getValue(),
                    party.getVisibility().name(),
                    party.getJoinPolicy().name(),
                    party.getStatus().name(),
                    party.getMaxMembers()
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
        public static Info from(Party party) {
            return new Info(
                    party.getId(),
                    party.getPlayerId(),
                    party.getName().getOriginal(),
                    party.getCode().getValue(),
                    party.getVisibility().name(),
                    party.getJoinPolicy().name(),
                    party.getStatus().name(),
                    party.getMaxMembers(),
                    party.getTags().stream().toList(),
                    party.getDescription() == null ? null : party.getDescription().getMd(),
                    party.getBanner() == null ? null : party.getBanner().getImageUrl(),
                    party.getBanner() == null ? null : party.getBanner().getBgColor(),
                    party.getLeaderPlayerId(),
                    party.getCreatedAt(),
                    party.getUpdatedAt()
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
