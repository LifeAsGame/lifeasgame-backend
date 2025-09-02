package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@AggregateRoot
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AbstractTime {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Email email;

    @Embedded
    private HashedPassword passwordHash;

    @Embedded
    private Nickname nickname;

    @Enumerated(EnumType.STRING)
    @Column(length=20, nullable=false)
    private UserStatus status;

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private User(Long id, Email email, HashedPassword passwordHash, Nickname nickname, UserStatus status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.status = status;
    }

    public static User register(Email email, HashedPassword passwordHash, Nickname nickname) {
        return new User(null, email, passwordHash, nickname, UserStatus.ACTIVE);
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    public void recordEvent(DomainEvent e) { domainEvents.add(e); }
}
