package online.lifeasgame.quest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardProfileRef {

    public static final int MAX_CODE_LENGTH = 80;

    @Column(name = "reward_profile_code", length = MAX_CODE_LENGTH)
    private String code;

    private RewardProfileRef(String code) {
        if (code == null || code.isBlank()) {
            throw new DomainException(QuestError.QUEST_REWARD_PROFILE_CODE_REQUIRED);
        }
        String normalized = code.trim();
        if (normalized.length() > MAX_CODE_LENGTH) {
            throw new DomainException(QuestError.QUEST_REWARD_PROFILE_CODE_TOO_LONG);
        }
        this.code = normalized;
    }

    public static RewardProfileRef of(String code) {
        return new RewardProfileRef(code);
    }

    public String code() {
        return code;
    }
}
