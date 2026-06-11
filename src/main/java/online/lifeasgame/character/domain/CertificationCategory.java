package online.lifeasgame.character.domain;

import java.util.List;
import online.lifeasgame.character.domain.error.CertificationError;
import online.lifeasgame.core.lang.EnumParsers;

public enum CertificationCategory {
    PROGRAMMING,
    CLOUD,
    DATABASE,
    SECURITY,
    DATA,
    NETWORK,
    LANGUAGE,
    MANAGEMENT,
    FINANCE,
    DESIGN,
    OTHER
    ;
    
    public static CertificationCategory parse(String raw) {
        return EnumParsers.parseStrict(
                CertificationCategory.class,
                raw,
                CertificationError.INVALID_CERTIFICATION_CATEGORY,
                "Certification Category"
        );
    }

    public static List<CertificationCategory> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                CertificationCategory.class,
                raw,
                CertificationError.INVALID_CERTIFICATION_CATEGORY,
                "Certification Categories"
        );
    }
}
