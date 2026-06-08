package online.lifeasgame.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.user.domain.error.UserError;

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
        // 메일 인증 활성화 시 아래로 교체
        // return new User(email, passwordHash, nickname, UserStatus.PENDING_EMAIL_VERIFICATION);
    }

    public static User registerByOAuth(Email email, Nickname nickname) {
        return new User(email, HashedPassword.oauthPlaceholder(), nickname, UserStatus.ACTIVE);
    }

    // ── 메일 인증 ──────────────────────────────────────────────────────────────

    /**
     * 이메일 인증 완료 처리.
     * PENDING_EMAIL_VERIFICATION → ACTIVE
     */
    public void verifyEmail() {
        if (this.status != UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new DomainException(UserError.INVALID_STATUS_CHANGE);
        }
        this.status = UserStatus.ACTIVE;
    }

    public boolean isPendingVerification() {
        return this.status == UserStatus.PENDING_EMAIL_VERIFICATION;
    }

    public boolean isOAuthAccount() {
        return this.passwordHash != null && this.passwordHash.isOAuthAccount();
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

    public void changePassword(
            HashedPassword currentPassword,
            HashedPassword newPassword
    ) {
        if (this.passwordHash.equals(currentPassword)) {
            this.passwordHash = newPassword;
        } else {
            throw new DomainException(UserError.INCORRECT_PASSWORD);
        }
    }

    public void changeStatus(UserStatus newStatus) {
        if (this.status == UserStatus.DELETED) {
            throw new DomainException(UserError.USER_ALREADY_DELETED);
        }

        if (this.status == newStatus) {
            return;
        }

        switch (newStatus) {
            case ACTIVE -> activate();
            case BANNED -> ban();
            case DELETED -> deleteBySystem();
            default -> throw new DomainException(UserError.INVALID_STATUS_CHANGE);
        }
    }

    private void activate() {
        this.status = UserStatus.ACTIVE;
    }

    private void ban() {
        if (this.status == UserStatus.DELETED) {
            throw new DomainException(UserError.INVALID_STATUS_CHANGE);
        }
        this.status = UserStatus.BANNED;
    }

    private void deleteBySystem() {
        this.status = UserStatus.DELETED;
    }

    public void delete(HashedPassword password) {
        if (this.passwordHash.equals(password)) {
            this.status = UserStatus.DELETED;
        }
    }
}
