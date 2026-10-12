package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/** Membership-scoped activity grant; commands and read projections are persisted by GroupActivityStore. */
@Entity
@Table(name = "group_activity_editors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupActivityEditor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "party_id")
    private Long partyId;
    @Column(name = "role_party_id")
    private Long rolePartyId;
    @Column(name = "player_id", nullable = false)
    private Long playerId;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(name = "member_joined_at", nullable = false)
    private LocalDateTime memberJoinedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
