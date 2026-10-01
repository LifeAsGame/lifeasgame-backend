package online.lifeasgame.social.application;

import online.lifeasgame.social.domain.RoleParty;
import online.lifeasgame.social.domain.RolePartyInvitation;
import online.lifeasgame.social.domain.RolePartyMember;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

public final class RolePartyResult {
    private RolePartyResult() {}

    public record Summary(Long id, String name, String description, String status,
                          Long creatorPlayerId, Long leaderPlayerId, int memberCount, int maxMembers,
                          Instant createdAt, Instant updatedAt) {
        static Summary from(RoleParty party) {
            return new Summary(party.getId(), party.getName(), party.getDescription(), party.getStatus().name(),
                    party.getCreatorPlayerId(), party.getLeaderPlayerId(), party.memberCount(), party.getMaxMembers(),
                    party.getCreatedAt(), party.getUpdatedAt());
        }
    }

    public record Detail(Long id, String name, String description, String status,
                         Long creatorPlayerId, Long leaderPlayerId, int memberCount, int maxMembers,
                         List<Member> members, Instant createdAt, Instant updatedAt) {
        static Detail from(RoleParty party) {
            return new Detail(party.getId(), party.getName(), party.getDescription(), party.getStatus().name(),
                    party.getCreatorPlayerId(), party.getLeaderPlayerId(), party.memberCount(), party.getMaxMembers(),
                    party.activeMembers().stream().map(Member::from).toList(), party.getCreatedAt(), party.getUpdatedAt());
        }
    }

    public record MyGroup(Summary group, String membershipStatus) {
        static MyGroup from(RoleParty party, Long playerId) {
            RolePartyMember member = party.getMembers().stream()
                    .filter(value -> value.getPlayerId().equals(playerId)).findFirst().orElseThrow();
            return new MyGroup(Summary.from(party), member.isActive() ? "ACTIVE" : "LEFT");
        }
    }

    public record Member(Long playerId, String role, Instant joinedAt) {
        static Member from(RolePartyMember member) {
            return new Member(member.getPlayerId(),
                    member.getParty().getLeaderPlayerId().equals(member.getPlayerId()) ? "LEADER" : "MEMBER",
                    member.getJoinedAt());
        }
    }

    public record Invitation(Long invitationId, Long rolePartyId, String groupName,
                             Long inviterPlayerId, Long inviteePlayerId, String status, Instant expiresAt) {
        static Invitation from(RolePartyInvitation invitation) {
            return new Invitation(invitation.getId(), invitation.getParty().getId(), invitation.getParty().getName(),
                    invitation.getInviterPlayerId(), invitation.getInviteePlayerId(), invitation.getStatus().name(), invitation.getExpiresAt());
        }
    }

    public record PageResult<T>(List<T> contents, int page, int size, long totalElements, int totalPages) {
        static <T> PageResult<T> of(Page<?> source, List<T> contents) {
            return new PageResult<>(contents, source.getNumber(), source.getSize(), source.getTotalElements(), source.getTotalPages());
        }
    }
}
