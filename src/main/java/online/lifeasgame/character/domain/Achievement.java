package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@AggregateRoot
@Table(name="achievements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Achievement extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, nullable = false, unique = true)
    private String code;

    @Column(length = 60, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private AchievementCategory category;

    @Lob
    @Column(name = "desc_md")
    private String descMd;

    public Achievement(String code, String name, AchievementCategory category, String descMd) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.descMd = descMd;
    }

    public static Achievement of(String code, String name, AchievementCategory category, String descMd) {
        return new Achievement(code, name, category, descMd);
    }
}
