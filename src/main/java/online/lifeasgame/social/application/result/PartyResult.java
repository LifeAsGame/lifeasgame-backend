package online.lifeasgame.social.application.result;

import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyMember;
import online.lifeasgame.social.domain.PartyWaitMember;
import online.lifeasgame.social.domain.repository.PartyRepository;

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
    
    public record MyParty(Long id, String name, String code, String status, String myRole, int memberCount, int maxMembers) {
        public static MyParty from(PartyRepository.MyParty row) {
            return new MyParty(row.id(), row.name(), row.code(), row.status().name(),
                    row.role().name(), Math.toIntExact(row.memberCount()), row.maxMembers());
        }
    }

    public record Member(Long playerId, String role, String joinedAt) {
        public static Member from(PartyMember member) {
            return new Member(member.getPlayerId(), member.getRole().name(), member.getJoinedAt().toString());
        }
    }

    public record Pending(Long id, Long partyId, String name, String code, Long playerId, String type, String status,
                          String message, String requestedAt, String expiresAt) {
        public static Pending from(PartyWaitMember wait) {
            Party group = wait.getParty();
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
