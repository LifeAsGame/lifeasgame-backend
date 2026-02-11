package online.lifeasgame.social.api.player.response;

import java.time.Instant;
import java.util.List;

public final class PlayerGuildResponse {

    private PlayerGuildResponse() {}

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

    public record MyGuild(
            Long id,
            String name,
            String code,
            String status,
            String myRole,
            int memberCount,
            int maxMembers
    ) {}

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

    public record Member(
            Long playerId,
            String role,
            String joinedAt
    ) {}

    public record WaitMember(
            Long id,
            Long playerId,
            String type,
            String status,
            String message,
            String requestedAt,
            String expiresAt
    ) {}

    public record Page<T>(
            List<T> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
