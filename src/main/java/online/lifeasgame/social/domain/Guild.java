package online.lifeasgame.social.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Getter
@Entity
@AggregateRoot
@Table(name = "guilds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guild extends AbstractTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private GuildName name;

    @AttributeOverride(name = "text", column = @Column(name = "desc_md", columnDefinition = "LONGTEXT"))
    private Markdown descMd;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(name = "capacity", nullable = false)
    private int capacity = 50;

    @Version
    private Long version;

    private Guild(GuildName name, Markdown descMd, Long leaderId, int capacity) {
        this.name = name;
        this.descMd = descMd;
        this.leaderId = leaderId;
        changeCapacity(capacity);
    }

    public static Guild create(String name, String descMd, Long leaderId, int capacity) {
        Guard.notNull(leaderId, "leaderId");
        return new Guild(GuildName.of(name), Markdown.of(descMd), leaderId, capacity);
    }

    public void rename(String newName) {
        this.name = GuildName.of(newName);
    }

    public void changeDescription(String md) {
        this.descMd = Markdown.of(md);
    }

    public void changeLeader(Long newLeaderId) {
        Guard.notNull(newLeaderId, "newLeaderId");
        this.leaderId = newLeaderId;
    }

    public void changeCapacity(int cap) {
        Guard.inRange(cap, 1, 500, "capacity");
        this.capacity = cap;
    }
}
