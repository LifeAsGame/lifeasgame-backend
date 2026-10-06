package online.lifeasgame.lifelog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.domain.error.LifeLogError;

import java.util.Locale;
import java.text.Normalizer;

@Entity
@Table(name = "lifelog_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LifeLogCategory {
    public enum Source { SYSTEM, PERSONAL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_player_id", nullable = false, updatable = false)
    private Long ownerPlayerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private LifeLogCategoryKind kind;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Source source;
    @Column(name = "system_code", length = 20, updatable = false)
    private String systemCode;
    @Column(length = 80)
    private String name;
    @Column(name = "normalized_name", length = 80)
    private String normalizedName;
    @Column(nullable = false)
    private boolean hidden;

    public static LifeLogCategory personal(Long owner, LifeLogCategoryKind kind, String name) {
        LifeLogCategory category = new LifeLogCategory();
        category.ownerPlayerId = owner;
        category.kind = kind;
        category.source = Source.PERSONAL;
        category.rename(name);
        return category;
    }

    public void rename(String value) {
        if (source != Source.PERSONAL || value == null) throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 80) throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        String normalized = normalize(trimmed);
        if (normalized.length() > 80) throw new DomainException(LifeLogError.INVALID_PERSONAL_CATEGORY);
        this.name = trimmed;
        this.normalizedName = normalized;
    }

    public static String normalize(String name) {
        return Normalizer.normalize(name.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    public void hide() { this.hidden = true; }
    public void show() { this.hidden = false; }
}
