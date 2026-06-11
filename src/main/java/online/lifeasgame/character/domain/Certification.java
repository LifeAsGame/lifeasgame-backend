package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@Table(name = "certification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certification extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "issuer", nullable = false)
    private String issuer;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private CertificationCategory category;

    private Certification(
            String name,
            String issuer,
            CertificationCategory category
    ) {
        this.name = name;
        this.issuer = issuer;
        this.category = category;
    }

    public static Certification create(
            String name,
            String issuer,
            CertificationCategory category
    ) {
        return new Certification(name, issuer, category);
    }

    public void change(
            String name,
            String issuer,
            CertificationCategory category
    ) {
        this.name = name;
        this.issuer = issuer;
        this.category = category;
    }

    public void update(String name, String issuer, CertificationCategory category) {
        this.name = name;
        this.issuer = issuer;
        this.category = category;
    }
}
