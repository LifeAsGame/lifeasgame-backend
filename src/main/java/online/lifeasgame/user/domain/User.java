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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;

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

    @Column(name="password_hash", length=200, nullable=false)
    private String passwordHash;

    @Embedded
    private Nickname nickname;

    @Enumerated(EnumType.STRING)
    @Column(length=20, nullable=false)
    private UserStatus status = UserStatus.ACTIVE;

    private User(Long id, Email email, String passwordHash, Nickname nickname, UserStatus status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.status = status;
    }

    public static User register(String email, String passwordHash, String nickname) {
        return new User(null, Email.of(email), passwordHash, Nickname.of(nickname), UserStatus.ACTIVE);
    }
}
