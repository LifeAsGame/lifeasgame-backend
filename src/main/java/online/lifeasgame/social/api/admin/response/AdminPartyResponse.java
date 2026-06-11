package online.lifeasgame.social.api.admin.response;

import java.time.Instant;
import java.util.List;

public final class AdminPartyResponse {

    private AdminPartyResponse() {}

    public record Summary(
            Long id,
            String name,
            String code,
            String visibility,
            String joinPolicy,
            String status,
            int maxMembers
    ) {
    }

    public record Detail(
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
    }


    public record Page<T>(
            List<T> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
