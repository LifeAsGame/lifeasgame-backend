package online.lifeasgame.logs.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "coop_history",
        indexes = {
                @Index(name = "idx_party_time3", columnList = "party_id,created_at"),
                @Index(name = "idx_player_time3", columnList = "player_id,created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoopHistory extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "context", columnDefinition = "json"))
    private JsonBlob context;

    private CoopHistory(Long playerId, Long partyId, Long partnerId, JsonBlob context) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.partyId = partyId;
        this.partnerId = partnerId;
        this.context = context;
    }

    public static CoopHistory record(Long playerId, Long partyId, Long partnerId, String contextJson) {
        return new CoopHistory(playerId, partyId, partnerId, contextJson == null ? null : JsonBlob.of(contextJson));
    }
}
