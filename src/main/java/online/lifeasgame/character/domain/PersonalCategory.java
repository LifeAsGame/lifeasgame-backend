package online.lifeasgame.character.domain;

import jakarta.persistence.*;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.character.domain.error.PersonalCategoryError;
import online.lifeasgame.core.error.DomainException;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "personal_categories", uniqueConstraints = @UniqueConstraint(
        name = "uq_personal_category_name", columnNames = {"owner_player_id", "kind", "normalized_name"}))
public class PersonalCategory {
    public enum Kind { CERTIFICATION, HOBBY }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_player_id", nullable = false)
    private Long ownerPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 80)
    private String normalizedName;

    public PersonalCategory(Long ownerPlayerId, Kind kind, String name) {
        this.ownerPlayerId = ownerPlayerId;
        this.kind = kind;
        rename(name);
    }

    public void rename(String name) {
        String trimmed = name == null ? null : name.strip();
        if (trimmed == null || trimmed.isBlank() || trimmed.codePointCount(0, trimmed.length()) > 80) {
            throw new DomainException(PersonalCategoryError.INVALID_NAME);
        }
        this.name = trimmed;
        this.normalizedName = this.name.toLowerCase(Locale.ROOT);
        if (this.normalizedName.codePointCount(0, this.normalizedName.length()) > 80) {
            throw new DomainException(PersonalCategoryError.INVALID_NAME);
        }
    }
}
