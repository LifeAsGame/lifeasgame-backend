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
import java.time.Instant;

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

    @Column(name = "issuer")
    private String issuer;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private CertificationCategory category;

    @Column(length = 32)
    private String provider;

    @Column(name = "source_code", length = 64)
    private String sourceCode;

    @Column(name = "major_code", length = 32)
    private String majorCode;

    @Column(name = "major_name", length = 120)
    private String majorName;

    @Column(name = "minor_code", length = 32)
    private String minorCode;

    @Column(name = "minor_name", length = 120)
    private String minorName;

    @Column(name = "administering_agency")
    private String administeringAgency;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "detail_status", length = 20)
    private String detailStatus;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

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

    public static Certification official() {
        return new Certification();
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

    public void importOfficial(String sourceCode, String name, String issuer, String majorCode,
                               String majorName, String minorCode, String minorName, String administeringAgency,
                               String detail, String detailStatus, String sourceUrl, Instant fetchedAt) {
        this.provider = "HRDK";
        this.sourceCode = sourceCode;
        this.name = name;
        this.issuer = issuer;
        this.category = CertificationCategory.OTHER;
        this.majorCode = majorCode;
        this.majorName = majorName;
        this.minorCode = minorCode;
        this.minorName = minorName;
        this.administeringAgency = administeringAgency;
        this.detail = detail;
        this.detailStatus = detailStatus;
        this.sourceUrl = sourceUrl;
        this.fetchedAt = fetchedAt;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
