package online.lifeasgame.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * The run registry writes this row with JDBC. Mapping the owned actor identity also
 * keeps create-drop test schemas aligned with the production Flyway table.
 */
@Entity
@Table(name = "portfolio_demo_actors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemoActorMembership {
    @EmbeddedId
    private Key key;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "player_id", unique = true)
    private Long playerId;

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {
        @Column(name = "run_id", nullable = false, columnDefinition = "char(36)")
        private String runId;

        @Column(name = "actor", nullable = false, length = 20)
        private String actor;
    }
}
