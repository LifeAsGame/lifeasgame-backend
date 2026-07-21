package online.lifeasgame.reward.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.reward.domain.error.RewardError;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Entity
@AggregateRoot
@Table(name = "reward_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardProfile extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80, nullable = false, unique = true)
    private String code;

    @Column(length = 120, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RewardProfileStatus status;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<RewardProfileLine> lines = new ArrayList<>();

    private RewardProfile(String code, String name, RewardProfileStatus status) {
        this.code = normalizeRequired(
                code,
                80,
                RewardError.REWARD_PROFILE_CODE_REQUIRED,
                RewardError.REWARD_PROFILE_CODE_TOO_LONG
        );
        this.name = normalizeRequired(
                name,
                120,
                RewardError.REWARD_PROFILE_NAME_REQUIRED,
                RewardError.REWARD_PROFILE_NAME_TOO_LONG
        );
        this.status = status == null ? RewardProfileStatus.INACTIVE : status;
    }

    public static RewardProfile create(String code, String name, RewardProfileStatus status) {
        return new RewardProfile(code, name, status);
    }

    public RewardProfileLine addLine(
            RewardDefinition rewardDefinition,
            int sortOrder,
        Long amountOverride
    ) {
        if (lines.stream().anyMatch(line -> line.getSortOrder() == sortOrder)) {
            throw new DomainException(RewardError.REWARD_LINE_SORT_ORDER_DUPLICATED);
        }
        RewardProfileLine line = RewardProfileLine.create(this, rewardDefinition, sortOrder, amountOverride);
        lines.add(line);
        lines.sort(Comparator.comparingInt(RewardProfileLine::getSortOrder));
        return line;
    }

    public List<RewardProfileLine> getLines() {
        return List.copyOf(lines);
    }

    public boolean isActive() {
        return status == RewardProfileStatus.ACTIVE;
    }

    public void assertActive() {
        if (!isActive()) {
            throw new DomainException(RewardError.REWARD_PROFILE_INACTIVE);
        }
    }

    public void activate() {
        this.status = RewardProfileStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = RewardProfileStatus.INACTIVE;
    }

    private static String normalizeRequired(
            String value,
            int maxLength,
            RewardError requiredError,
            RewardError tooLongError
    ) {
        if (value == null || value.isBlank()) {
            throw new DomainException(requiredError);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new DomainException(tooLongError);
        }
        return normalized;
    }
}
