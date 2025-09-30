package online.lifeasgame.core.lang;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.error.ErrorCode;

import java.util.*;
import java.util.stream.Collectors;

public final class EnumParsers {

    private EnumParsers() {}

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    public static <E extends Enum<E>> E parseStrict(
            Class<E> enumType,
            String raw,
            ErrorCode errorCode,
            String fieldLabel
    ) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String norm = normalize(raw);
        if (norm.isEmpty()) {
            throw new DomainException(errorCode, fieldLabel + " is blank");
        }

        try {
            return Enum.valueOf(enumType, norm);
        } catch (IllegalArgumentException ex) {
            throw new DomainException(
                    errorCode,
                    "Invalid " + fieldLabel + ": " + raw + " (allowed: " + allowedList(enumType) + ")"
            );
        }
    }

    public static <E extends Enum<E>> Optional<E> parseOptional(
            Class<E> enumType,
            String raw
    ) {
        if (raw == null) return Optional.empty();
        String norm = normalize(raw);
        if (norm.isEmpty()) return Optional.empty();
        try {
            return Optional.of(Enum.valueOf(enumType, norm));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static <E extends Enum<E>> List<E> parseListStrict(
            Class<E> enumType,
            List<String> raws,
            ErrorCode errorCode,
            String fieldLabel
    ) {
        if (raws == null || raws.isEmpty()) {
            return List.of();
        }

        List<String> invalids = new ArrayList<>();
        LinkedHashSet<E> parsed = new LinkedHashSet<>();

        for (String s : raws) {
            if (s == null) continue;
            String norm = normalize(s);
            if (norm.isEmpty()) continue;
            try {
                parsed.add(Enum.valueOf(enumType, norm));
            } catch (IllegalArgumentException ex) {
                invalids.add(s);
            }
        }

        if (!invalids.isEmpty()) {
            throw new DomainException(
                    errorCode,
                    "Invalid " + fieldLabel + ": " + invalids + " (allowed: " + allowedList(enumType) + ")"
            );
        }

        return List.copyOf(parsed);
    }

    private static <E extends Enum<E>> String allowedList(Class<E> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
