package online.lifeasgame.social.api.player.response;

import java.time.Instant;
import java.util.List;

public final class PlayerPartyResponse {

    private PlayerPartyResponse() {}

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

    public record MyParty(
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

    public record Pending(Long id, Long partyId, String name, String code, Long playerId, String type, String status,
                          String message, String requestedAt, String expiresAt) {}

    public record Me(String myRole, boolean pendingJoin, boolean pendingInvitation, List<String> actions) {}

    public record Page<T>(
            List<T> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
