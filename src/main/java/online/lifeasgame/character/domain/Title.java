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
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@Table(name="titles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Title extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, nullable = false, unique = true)
    private String code;

    @Column(length = 60, nullable = false)
    private String name;

    @Lob
    @Column(name = "desc_md")
    private String descMd;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TitleCategory category;
}
