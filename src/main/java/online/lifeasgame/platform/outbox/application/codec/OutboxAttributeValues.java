package online.lifeasgame.platform.outbox.application.codec;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

final class OutboxAttributeValues {

    private OutboxAttributeValues() {
    }

    static Map<String, Value> encode(Map<String, Object> attributes) {
        Map<String, Value> encoded = new LinkedHashMap<>();
        attributes.forEach((key, value) -> encoded.put(key, encodeValue(value)));
        return Map.copyOf(encoded);
    }

    static Map<String, Object> decode(Map<String, Value> attributes) {
        Map<String, Object> decoded = new LinkedHashMap<>();
        attributes.forEach((key, value) -> decoded.put(key, decodeValue(value)));
        return Map.copyOf(decoded);
    }

    private static Value encodeValue(Object value) {
        if (value == null) {
            return new Value("null", null, null, null);
        }
        if (value instanceof String string) {
            return scalar("string", string);
        }
        if (value instanceof Character character) {
            return scalar("string", character.toString());
        }
        if (value instanceof Boolean bool) {
            return scalar("boolean", bool.toString());
        }
        if (value instanceof Byte number) {
            return scalar("byte", number.toString());
        }
        if (value instanceof Short number) {
            return scalar("short", number.toString());
        }
        if (value instanceof Integer number) {
            return scalar("integer", number.toString());
        }
        if (value instanceof Long number) {
            return scalar("long", number.toString());
        }
        if (value instanceof Float number) {
            return scalar("float", number.toString());
        }
        if (value instanceof Double number) {
            return scalar("double", number.toString());
        }
        if (value instanceof BigInteger number) {
            return scalar("big-integer", number.toString());
        }
        if (value instanceof BigDecimal number) {
            return scalar("big-decimal", number.toPlainString());
        }
        if (value instanceof Instant instant) {
            return scalar("instant", instant.toString());
        }
        if (value instanceof Enum<?> enumValue) {
            return scalar("enum-name", enumValue.name());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Value> object = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                if (!(key instanceof String stringKey)) {
                    throw unsupported(value);
                }
                object.put(stringKey, encodeValue(nested));
            });
            return new Value("map", null, Map.copyOf(object), null);
        }
        if (value instanceof Collection<?> collection) {
            return new Value(
                    "list",
                    null,
                    null,
                    collection.stream()
                            .map(OutboxAttributeValues::encodeValue)
                            .toList()
            );
        }
        throw unsupported(value);
    }

    private static Object decodeValue(Value value) {
        return switch (value.type()) {
            case "null" -> null;
            case "string", "enum-name" -> value.scalar();
            case "boolean" -> Boolean.valueOf(value.scalar());
            case "byte" -> Byte.valueOf(value.scalar());
            case "short" -> Short.valueOf(value.scalar());
            case "integer" -> Integer.valueOf(value.scalar());
            case "long" -> Long.valueOf(value.scalar());
            case "float" -> Float.valueOf(value.scalar());
            case "double" -> Double.valueOf(value.scalar());
            case "big-integer" -> new BigInteger(value.scalar());
            case "big-decimal" -> new BigDecimal(value.scalar());
            case "instant" -> Instant.parse(value.scalar());
            case "map" -> decode(value.object());
            case "list" -> value.list().stream()
                    .map(OutboxAttributeValues::decodeValue)
                    .toList();
            default -> throw unsupported(value.type());
        };
    }

    private static Value scalar(String type, String scalar) {
        return new Value(type, scalar, null, null);
    }

    private static DomainException unsupported(Object value) {
        return new DomainException(
                OutboxError.OUTBOX_EVENT_ATTRIBUTE_TYPE_UNSUPPORTED,
                value == null ? null : value.getClass().getSimpleName()
        );
    }

    record Value(
            String type,
            String scalar,
            Map<String, Value> object,
            List<Value> list
    ) {
    }
}
