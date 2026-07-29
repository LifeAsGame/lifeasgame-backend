package online.lifeasgame.platform.outbox.application.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class LifeLogRecordedOutboxCodec
        implements OutboxEventCodec<LifeLogRecorded> {

    static final String ALIAS = "lifelog.recorded.v1";

    private static final Set<String> NEW_FIELDS = Set.of(
            "eventId",
            "eventType",
            "eventVersion",
            "occurredAt",
            "playerId",
            "lifeLogId",
            "sourceDefinitionVersion",
            "subtype",
            "entryMode",
            "reflectionScope",
            "periodKey",
            "primaryRoleId"
    );
    private static final Set<String> LEGACY_REQUIRED_FIELDS = Set.of(
            "eventId",
            "eventVersion",
            "playerId",
            "lifeLogId",
            "lifeLogType",
            "occurredAt"
    );
    private static final Set<String> LEGACY_FIELDS = Set.of(
            "eventId",
            "eventVersion",
            "playerId",
            "lifeLogId",
            "lifeLogType",
            "primaryRoleId",
            "occurredAt"
    );

    private final ObjectMapper objectMapper;

    LifeLogRecordedOutboxCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String alias() {
        return ALIAS;
    }

    @Override
    public Class<LifeLogRecorded> eventType() {
        return LifeLogRecorded.class;
    }

    @Override
    public String encode(LifeLogRecorded event) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("eventId", event.eventId());
            payload.put("eventType", event.eventType());
            payload.put("eventVersion", event.eventVersion());
            payload.put("occurredAt", event.occurredAt().toString());
            payload.put("playerId", event.playerId());
            payload.put("lifeLogId", event.lifeLogId());
            putNullable(
                    payload,
                    "sourceDefinitionVersion",
                    event.sourceDefinitionVersion()
            );
            putNullable(
                    payload,
                    "subtype",
                    name(event.subtype())
            );
            putNullable(
                    payload,
                    "entryMode",
                    name(event.entryMode())
            );
            putNullable(
                    payload,
                    "reflectionScope",
                    name(event.reflectionScope())
            );
            putNullable(payload, "periodKey", event.periodKey());
            putNullable(payload, "primaryRoleId", event.primaryRoleId());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw failed(exception);
        }
    }

    @Override
    public LifeLogRecorded decode(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException(
                        "LifeLogRecorded payload must be an object"
                );
            }
            if (root.has("lifeLogType")) {
                return decodeLegacy(root);
            }
            return decodeNew(root);
        } catch (JsonProcessingException exception) {
            throw failed(exception);
        } catch (RuntimeException exception) {
            throw failed(exception);
        }
    }

    private LifeLogRecorded decodeLegacy(JsonNode root) {
        Set<String> fields = fieldNames(root);
        if (!LEGACY_FIELDS.containsAll(fields)
                || !fields.containsAll(LEGACY_REQUIRED_FIELDS)) {
            throw new IllegalArgumentException(
                    "Unknown or missing legacy LifeLogRecorded field"
            );
        }
        return LifeLogRecorded.legacy(
                requiredText(root, "eventId"),
                requiredInt(root, "eventVersion"),
                requiredLong(root, "playerId"),
                requiredLong(root, "lifeLogId"),
                LifeLogType.valueOf(requiredText(root, "lifeLogType")),
                nullableLong(root, "primaryRoleId"),
                Instant.parse(requiredText(root, "occurredAt"))
        );
    }

    private LifeLogRecorded decodeNew(JsonNode root) {
        if (!fieldNames(root).equals(NEW_FIELDS)) {
            throw new IllegalArgumentException(
                    "Unknown or missing LifeLogRecorded field"
            );
        }
        return new LifeLogRecorded(
                requiredText(root, "eventId"),
                requiredText(root, "eventType"),
                requiredInt(root, "eventVersion"),
                Instant.parse(requiredText(root, "occurredAt")),
                requiredLong(root, "playerId"),
                requiredLong(root, "lifeLogId"),
                nullableInt(root, "sourceDefinitionVersion"),
                nullableEnum(root, "subtype", LifeLogSubtype.class),
                nullableEnum(root, "entryMode", LifeLogEntryMode.class),
                nullableEnum(
                        root,
                        "reflectionScope",
                        LifeLogReflectionScope.class
                ),
                nullableText(root, "periodKey"),
                nullableLong(root, "primaryRoleId"),
                null
        );
    }

    private static Set<String> fieldNames(JsonNode root) {
        Set<String> fields = new HashSet<>();
        Iterator<String> names = root.fieldNames();
        names.forEachRemaining(fields::add);
        return fields;
    }

    private static String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " must be text");
        }
        return value.asText();
    }

    private static String nullableText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException(field + " must be text");
        }
        return value.asText();
    }

    private static int requiredInt(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isIntegralNumber()) {
            throw new IllegalArgumentException(field + " must be integer");
        }
        return value.intValue();
    }

    private static Integer nullableInt(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be integer");
        }
        return value.intValue();
    }

    private static Long requiredLong(JsonNode root, String field) {
        Long value = nullableLong(root, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " must be integer");
        }
        return value;
    }

    private static Long nullableLong(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new IllegalArgumentException(field + " must be integer");
        }
        return value.longValue();
    }

    private static <E extends Enum<E>> E nullableEnum(
            JsonNode root,
            String field,
            Class<E> type
    ) {
        String value = nullableText(root, field);
        return value == null ? null : Enum.valueOf(type, value);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static void putNullable(
            ObjectNode target,
            String field,
            String value
    ) {
        if (value == null) {
            target.putNull(field);
        } else {
            target.put(field, value);
        }
    }

    private static void putNullable(
            ObjectNode target,
            String field,
            Integer value
    ) {
        if (value == null) {
            target.putNull(field);
        } else {
            target.put(field, value);
        }
    }

    private static void putNullable(
            ObjectNode target,
            String field,
            Long value
    ) {
        if (value == null) {
            target.putNull(field);
        } else {
            target.put(field, value);
        }
    }

    private static DomainException failed(Exception exception) {
        return new DomainException(
                OutboxError.OUTBOX_EVENT_CODEC_FAILED,
                null,
                exception
        );
    }
}
