package online.lifeasgame.quest.application.automation;

import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.*;

@Component
public class QuestSignalFingerprint {

    public String fingerprint(QuestSignal signal) {
        return fingerprintWithOccurredAt(signal, signal.occurredAt());
    }

    String fingerprintWithOccurredAt(
            QuestSignal signal,
            Instant occurredAt
    ) {
        StringBuilder canonical = new StringBuilder();
        appendCommon(canonical, signal, occurredAt);
        append(
                canonical,
                "acceptancePolicy",
                signal.acceptancePolicy().name()
        );
        append(canonical, "periodKey", signal.periodKey());
        append(canonical, "attributes", signal.attributes());
        return sha256(canonical.toString());
    }

    String legacyFingerprint(QuestSignal signal) {
        StringBuilder canonical = new StringBuilder();
        appendCommon(canonical, signal, signal.occurredAt());
        append(canonical, "attributes", signal.attributes());
        return sha256(canonical.toString());
    }

    private void appendCommon(
            StringBuilder canonical,
            QuestSignal signal,
            Instant occurredAt
    ) {
        append(canonical, "questCode", signal.questCode().value());
        append(canonical, "playerId", signal.playerId());
        append(canonical, "signalType", signal.type().name());
        if (signal.isSetOperation()) {
            append(canonical, "progressValue", signal.progressValue());
        } else {
            append(canonical, "progressDelta", signal.progressDelta());
        }
        append(canonical, "occurredAt", occurredAt);
    }

    private void append(StringBuilder target, String key, Object value) {
        target.append(key.length())
                .append(':')
                .append(key)
                .append('=');
        appendValue(target, value);
        target.append(';');
    }

    private void appendValue(StringBuilder target, Object value) {
        if (value == null) {
            target.append("null");
            return;
        }
        if (value instanceof CharSequence text) {
            appendText(target, "string", text.toString());
            return;
        }
        if (value instanceof Number number) {
            target.append("number:")
                    .append(normalizeNumber(number));
            return;
        }
        if (value instanceof Boolean bool) {
            target.append("boolean:").append(bool);
            return;
        }
        if (value instanceof Enum<?> enumValue) {
            appendText(target, "enum", enumValue.name());
            return;
        }
        if (value instanceof TemporalAccessor temporal) {
            appendText(target, "temporal", temporal.toString());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            appendMap(target, map);
            return;
        }
        if (value instanceof Collection<?> collection) {
            appendCollection(target, collection);
            return;
        }
        if (value.getClass().isArray()) {
            appendArray(target, value);
            return;
        }
        appendText(target, value.getClass().getName(), value.toString());
    }

    private void appendMap(StringBuilder target, Map<?, ?> map) {
        target.append("map:{");
        map.entrySet().stream()
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .forEach(entry -> {
                    appendValue(target, String.valueOf(entry.getKey()));
                    target.append('=');
                    appendValue(target, entry.getValue());
                    target.append(',');
                });
        target.append('}');
    }

    private void appendCollection(
            StringBuilder target,
            Collection<?> collection
    ) {
        target.append("list:[");
        collection.forEach(value -> {
            appendValue(target, value);
            target.append(',');
        });
        target.append(']');
    }

    private void appendArray(StringBuilder target, Object array) {
        target.append("array:[");
        for (int index = 0; index < Array.getLength(array); index++) {
            appendValue(target, Array.get(array, index));
            target.append(',');
        }
        target.append(']');
    }

    private void appendText(StringBuilder target, String type, String value) {
        target.append(type)
                .append(':')
                .append(value.length())
                .append(':')
                .append(value);
    }

    private String normalizeNumber(Number number) {
        try {
            return new BigDecimal(number.toString())
                    .stripTrailingZeros()
                    .toPlainString();
        } catch (NumberFormatException exception) {
            return number.toString();
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
