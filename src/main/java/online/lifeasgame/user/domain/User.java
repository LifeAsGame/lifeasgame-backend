package online.lifeasgame.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.util.ArrayList;
import java.util.List;

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

    private User(Email email, HashedPassword passwordHash, Nickname nickname, UserStatus status) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.status = status;
    }

    public static User register(Email email, HashedPassword passwordHash, Nickname nickname) {
        return new User(email, passwordHash, nickname, UserStatus.ACTIVE);
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    public void recordEvent(DomainEvent e) { domainEvents.add(e); }

    public void changeNickname(Nickname nickname) {
        this.nickname = nickname;
    }
}
