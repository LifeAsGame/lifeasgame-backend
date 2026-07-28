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
public class QuestRoleTemplateRef {

    public static final int MAX_CODE_LENGTH = 80;

    @Column(name = "role_template_code", length = MAX_CODE_LENGTH)
    private String code;

    private QuestRoleTemplateRef(String code) {
        if (code == null || code.isBlank()) {
            throw new DomainException(
                    QuestError.QUEST_ROLE_TEMPLATE_CODE_REQUIRED
            );
        }
        String normalized = code.trim();
        if (normalized.length() > MAX_CODE_LENGTH) {
            throw new DomainException(
                    QuestError.QUEST_ROLE_TEMPLATE_CODE_TOO_LONG
            );
        }
        this.code = normalized;
    }

    public static QuestRoleTemplateRef of(String code) {
        return new QuestRoleTemplateRef(code);
    }

    public String code() {
        return code;
    }
}
