package online.lifeasgame.character.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import online.lifeasgame.character.domain.error.CertificationError;
import online.lifeasgame.core.error.DomainException;

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
        if (raw == null) {
            throw new DomainException(CertificationError.INVALID_CERTIFICATION_CATEGORY, "Certification category is null");
        }

        String norm = normalize(raw);

        if (norm.isEmpty()) {
            throw new DomainException(CertificationError.INVALID_CERTIFICATION_CATEGORY, "Certification category is blank");
        }

        try {
            return CertificationCategory.valueOf(norm);
        } catch (IllegalArgumentException e) {
            throw new DomainException(
                    CertificationError.INVALID_CERTIFICATION_CATEGORY,
                    "Invalid Certification category: " + raw + " (allowed: " + allowedList() + ")"
            );
        }
    }

    public static List<CertificationCategory> parse(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<String> invalid = new ArrayList<>();
        LinkedHashSet<CertificationCategory> parsed = new LinkedHashSet<>();

        for (String s : raw) {
            if (s == null) {
                continue;
            }

            String norm = normalize(s);

            if (norm.isEmpty()) {
                continue;
            }

            try {
                parsed.add(CertificationCategory.valueOf(norm));
            } catch (IllegalArgumentException e) {
                invalid.add(s);
            }
        }

        if (!invalid.isEmpty()) {
            throw new DomainException(
                    CertificationError.INVALID_CERTIFICATION_CATEGORY,
                    "Invalid Certification categories: " + invalid + " (allowed: " + allowedList() + ")"
            );
        }

        return List.copyOf(parsed);
    }

    private static String normalize(String s) {
        return s.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static String allowedList() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
