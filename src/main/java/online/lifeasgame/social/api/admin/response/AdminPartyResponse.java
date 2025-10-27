package online.lifeasgame.social.api.admin.response;

import java.time.Instant;
import java.util.List;

public final class AdminPartyResponse {
    public record Summary(
            Long id, String name, String code, String visibility, String joinPolicy, String status, int maxMembers
    ) {
        public static Summary of(
                Long id,
                String name,
                String code,
                String visibility,
                String joinPolicy,
                String status,
                int maxMembers
        ) {
            return new Summary(id, name, code, visibility, joinPolicy, status, maxMembers);
        }
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
        public static Detail of(
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
            return new Detail(
                    id,
                    playerId,
                    name,
                    code,
                    visibility,
                    joinPolicy,
                    status,
                    maxMembers,
                    tags,
                    descriptionMd,
                    bannerImageUrl,
                    bannerBgColor,
                    leaderPlayerId,
                    createdAt,
                    updatedAt
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
        public static AdminPartyResponse.Info of(
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
            return new AdminPartyResponse.Info(
                    id,
                    playerId,
                    name,
                    code,
                    visibility,
                    joinPolicy,
                    status,
                    maxMembers,
                    tags,
                    descriptionMd,
                    emblemImageUrl,
                    emblemBgColor,
                    leaderPlayerId,
                    createdAt,
                    updatedAt
            );
        }
    }


    public record Page<T>(List<T> contents, int page, int size, long totalElements, int totalPages) {
        public static <T> Page<T> of(List<T> contents, int page, int size, long totalElements, int totalPages) {
            return new Page<>(contents, page, size, totalElements, totalPages);
        }
    }
}
