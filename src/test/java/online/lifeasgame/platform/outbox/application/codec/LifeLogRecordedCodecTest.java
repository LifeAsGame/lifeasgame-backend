package online.lifeasgame.platform.outbox.application.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LifeLogRecorded Outbox Codec")
class LifeLogRecordedCodecTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T09:15:30.123456Z");

    private OutboxEventCodecRegistry registry;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        registry = new OutboxEventCodecRegistry(objectMapper);
    }

    @Test
    @DisplayName("stable alias와 모든 필드를 그대로 round-trip한다")
    void roundTripsEveryField() {
        LifeLogRecorded source = LifeLogRecorded.of(
                "d05c05d8-75a8-436d-b2f6-e0d63107927d",
                197L,
                199L,
                LifeLogType.MEDIA,
                OCCURRED_AT
        );

        OutboxEventEnvelope envelope = registry.encode(source);
        LifeLogRecorded decoded = (LifeLogRecorded) registry.decode(
                envelope.eventType(),
                envelope.payload()
        );

        assertThat(envelope.eventType()).isEqualTo("lifelog.recorded.v1");
        assertThat(envelope.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(decoded).isEqualTo(source);
        assertThat(decoded.eventVersion()).isEqualTo(1);
        assertThat(decoded.primaryRoleId()).isNull();
    }

    @Test
    @DisplayName("Alias와 payload에 Java class name을 노출하지 않는다")
    void doesNotExposeJavaClassName() {
        LifeLogRecorded event = LifeLogRecorded.of(
                "839d32ae-c695-4bb0-9d48-65372ac78e99",
                197L,
                199L,
                LifeLogType.COLLECTION,
                OCCURRED_AT
        );

        OutboxEventEnvelope envelope = registry.encode(event);

        assertThat(envelope.eventType())
                .doesNotContain(LifeLogRecorded.class.getSimpleName())
                .doesNotContain(LifeLogRecorded.class.getName());
        assertThat(envelope.payload())
                .doesNotContain(LifeLogRecorded.class.getSimpleName())
                .doesNotContain(LifeLogRecorded.class.getName());
    }
}
