package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Entity
@AggregateRoot
@Table(name = "parties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Party extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, nullable = false)
    private String name;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(name = "capacity", nullable = false)
    private int capacity = 4;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PartyState state = PartyState.OPEN;

    @Version
    private Long version;

    private Party(String name, Long leaderId, int capacity) {
        rename(name);
        this.leaderId = leaderId;
        changeCapacity(capacity);
    }

    public static Party create(String name, Long leaderId, int capacity) {
        Guard.notNull(leaderId, "leaderId");
        return new Party(name, leaderId, capacity);
    }

    public void rename(String newName) {
        Guard.notBlank(newName, "new party name");
        String newPartyName = Guard.maxLength(newName.strip(), 60, "new party name");
        this.name = newPartyName;
    }

    public void changeLeader(Long newLeader) {
        Guard.notNull(newLeader, "newLeader");
        this.leaderId = newLeader;
    }

    public void changeCapacity(int cap){
        Guard.inRange(cap, 2, 20, "capacity");
        this.capacity = cap;
    }

    public void disband() {
        this.state = PartyState.DISBANDED;
    }
}
