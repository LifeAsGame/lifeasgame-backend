package online.lifeasgame.platform.outbox.application.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LifeLogRecorded Outbox Codec")
class LifeLogRecordedCodecTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T09:15:30.123456Z");
    private static final Set<String> CONTENT_FIELDS = Set.of(
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

    private ObjectMapper objectMapper;
    private OutboxEventCodecRegistry registry;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        registry = new OutboxEventCodecRegistry(objectMapper);
    }

    @Test
    @DisplayName("stable alias와 Content 1B payload를 정확히 round-trip한다")
    void roundTripsContentPayload() throws Exception {
        LifeLogRecorded source = contentReadyEvent();

        OutboxEventEnvelope envelope = registry.encode(source);
        LifeLogRecorded decoded = (LifeLogRecorded) registry.decode(
                envelope.eventType(),
                envelope.payload()
        );
        JsonNode json = objectMapper.readTree(envelope.payload());
        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);

        assertThat(envelope.eventType()).isEqualTo("lifelog.recorded.v1");
        assertThat(envelope.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(fields).containsExactlyInAnyOrderElementsOf(CONTENT_FIELDS);
        assertThat(json.get("eventType").asText())
                .isEqualTo("LifeLogRecorded");
        assertThat(json.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(decoded).isEqualTo(source);
        assertThat(decoded.isContentReady()).isTrue();
    }

    @Test
    @DisplayName("과거 v1 JSON fixture를 decode하고 contentReady false로 표시한다")
    void decodesLegacyFixture() {
        String fixture = """
                {
                  "eventId": "d05c05d8-75a8-436d-b2f6-e0d63107927d",
                  "eventVersion": 1,
                  "playerId": 197,
                  "lifeLogId": 51,
                  "lifeLogType": "EXERCISE",
                  "primaryRoleId": null,
                  "occurredAt": "2026-07-24T09:15:30.123456Z"
                }
                """;

        LifeLogRecorded decoded = (LifeLogRecorded) registry.decode(
                "lifelog.recorded.v1",
                fixture
        );

        assertThat(decoded.eventType()).isEqualTo("LifeLogRecorded");
        assertThat(decoded.eventVersion()).isEqualTo(1);
        assertThat(decoded.lifeLogId()).isEqualTo(51L);
        assertThat(decoded.legacyLifeLogType())
                .isEqualTo(LifeLogType.EXERCISE);
        assertThat(decoded.sourceDefinitionVersion()).isNull();
        assertThat(decoded.isContentReady()).isFalse();
    }

    @Test
    @DisplayName("new encode는 physical source와 private content key를 노출하지 않는다")
    void excludesPhysicalAndPrivateFields() throws Exception {
        String payload = registry.encode(contentReadyEvent()).payload();
        JsonNode json = objectMapper.readTree(payload);

        assertThat(json.has("lifeLogType")).isFalse();
        assertThat(json.has("sourceType")).isFalse();
        assertThat(json.has("title")).isFalse();
        assertThat(json.has("body")).isFalse();
        assertThat(json.has("content")).isFalse();
        assertThat(json.has("memo")).isFalse();
        assertThat(json.has("tags")).isFalse();
        assertThat(payload).doesNotContain(LifeLogRecorded.class.getName());
    }

    @Test
    @DisplayName("unknown field와 invalid payload는 stable codec error로 실패한다")
    void rejectsUnknownAndInvalidPayload() {
        String unknown = registry.encode(contentReadyEvent()).payload()
                .replace(
                        "\"primaryRoleId\":null",
                        "\"primaryRoleId\":null,\"title\":\"private\""
                );
        String invalid = registry.encode(contentReadyEvent()).payload()
                .replace("\"eventType\":\"LifeLogRecorded\"",
                        "\"eventType\":\"Other\""
                );

        assertCodecFailure(unknown);
        assertCodecFailure(invalid);
        assertCodecFailure("{\"eventId\":\"incomplete\"}");
    }

    @Test
    @DisplayName("new와 legacy payload의 int 범위 밖 eventVersion을 거부한다")
    void rejectsOutOfRangeEventVersion() {
        String newPayload = registry.encode(contentReadyEvent()).payload()
                .replace(
                        "\"eventVersion\":1",
                        "\"eventVersion\":4294967297"
                );
        String legacyPayload = """
                {
                  "eventId": "d05c05d8-75a8-436d-b2f6-e0d63107927d",
                  "eventVersion": 4294967297,
                  "playerId": 197,
                  "lifeLogId": 51,
                  "lifeLogType": "EXERCISE",
                  "primaryRoleId": null,
                  "occurredAt": "2026-07-24T09:15:30.123456Z"
                }
                """;

        assertCodecFailure(newPayload);
        assertCodecFailure(legacyPayload);
    }

    @Test
    @DisplayName("int 범위 안의 unsupported eventVersion도 stable codec error로 거부한다")
    void rejectsUnsupportedInRangeEventVersion() {
        String payload = registry.encode(contentReadyEvent()).payload()
                .replace(
                        "\"eventVersion\":1",
                        "\"eventVersion\":2147483647"
                );

        assertCodecFailure(payload);
    }

    @Nested
    @DisplayName("v1 payload에 Role snapshot을 encode할 때")
    class EncodeRoleSnapshot {

        @Test
        @DisplayName("primaryRoleId만 활성화하고 roleEventId field는 추가하지 않는다")
        void keepsVersionOneShape() throws Exception {
            LifeLogRecorded source = new LifeLogRecorded(
                    "role-event",
                    LifeLogRecorded.EVENT_TYPE,
                    LifeLogRecorded.EVENT_VERSION,
                    OCCURRED_AT,
                    197L,
                    213L,
                    1,
                    LifeLogSubtype.MEMORY,
                    LifeLogEntryMode.FULL,
                    null,
                    null,
                    31L,
                    null
            );

            OutboxEventEnvelope envelope = registry.encode(source);
            JsonNode json = objectMapper.readTree(envelope.payload());
            LifeLogRecorded decoded = (LifeLogRecorded) registry.decode(
                    envelope.eventType(),
                    envelope.payload()
            );

            assertThat(json.get("eventVersion").asInt()).isEqualTo(1);
            assertThat(json.get("primaryRoleId").asLong()).isEqualTo(31L);
            assertThat(json.has("roleEventId")).isFalse();
            assertThat(decoded.primaryRoleId()).isEqualTo(31L);
        }
    }

    private void assertCodecFailure(String payload) {
        assertThatThrownBy(() -> registry.decode(
                "lifelog.recorded.v1",
                payload
        )).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(OutboxError.OUTBOX_EVENT_CODEC_FAILED)
        );
    }

    private LifeLogRecorded contentReadyEvent() {
        return new LifeLogRecorded(
                "d05c05d8-75a8-436d-b2f6-e0d63107927d",
                "LifeLogRecorded",
                1,
                OCCURRED_AT,
                197L,
                213L,
                1,
                LifeLogSubtype.REFLECTION,
                LifeLogEntryMode.QUICK,
                LifeLogReflectionScope.WEEKLY_LOOKBACK,
                "2026-W30",
                null,
                null
        );
    }
}
