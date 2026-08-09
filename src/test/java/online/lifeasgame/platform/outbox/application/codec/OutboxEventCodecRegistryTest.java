package online.lifeasgame.platform.outbox.application.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import online.lifeasgame.character.domain.event.PlayerLeveledUp;
import online.lifeasgame.character.domain.event.PlayerRegistered;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.domain.event.EconomyEventType;
import online.lifeasgame.inventory.domain.event.InventoryItemAdded;
import online.lifeasgame.lifelog.domain.event.CollectionLogged;
import online.lifeasgame.lifelog.domain.event.ExerciseLogged;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.MediaLogAdvanced;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.social.domain.ChatChannelType;
import online.lifeasgame.social.domain.event.ChatChannelDeactivated;
import online.lifeasgame.user.domain.event.UserRegistered;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OutboxEventCodecRegistry")
class OutboxEventCodecRegistryTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T07:00:00.123456Z");

    private OutboxEventCodecRegistry registry;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        registry = new OutboxEventCodecRegistry(objectMapper);
    }

    @Nested
    @DisplayName("등록된 DomainEvent를 변환할 때")
    class RoundTrip {

        @Test
        @DisplayName("모든 concrete DomainEvent가 stable alias로 round-trip된다")
        void roundTripsEveryDomainEvent() {
            List<DomainEvent> events = List.of(
                    new PlayerRegistered(197L, OCCURRED_AT),
                    new PlayerLeveledUp(197L, 1, 2, OCCURRED_AT),
                    new InventoryItemAdded(
                            197L,
                            31L,
                            "RARE",
                            true,
                            false,
                            2,
                            OCCURRED_AT
                    ),
                    new CollectionLogged(
                            197L,
                            41L,
                            "BOOK",
                            3,
                            OCCURRED_AT
                    ),
                    new ExerciseLogged(
                            197L,
                            51L,
                            "RUNNING",
                            30,
                            5.5,
                            250,
                            OCCURRED_AT
                    ),
                    new LifeLogRecorded(
                            "2a294fd2-1e08-49b9-a9f2-a3a4e3c17cb6",
                            "LifeLogRecorded",
                            1,
                            OCCURRED_AT,
                            197L,
                            52L,
                            1,
                            LifeLogSubtype.ACTIVITY,
                            LifeLogEntryMode.FULL,
                            null,
                            null,
                            null,
                            null
                    ),
                    new MediaLogAdvanced(
                            197L,
                            61L,
                            2,
                            4,
                            12,
                            OCCURRED_AT
                    ),
                    new UserRegistered(
                            71L,
                            "user@example.com",
                            "player",
                            OCCURRED_AT
                    ),
                    new ChatChannelDeactivated(
                            81L,
                            ChatChannelType.GUILD,
                            OCCURRED_AT,
                            "inactive"
                    ),
                    questEvent(),
                    rewardReadyFact(),
                    economyEvent()
            );

            assertThat(registry.size()).isEqualTo(events.size());
            for (DomainEvent event : events) {
                OutboxEventEnvelope envelope = registry.encode(event);

                assertThat(envelope.eventType()).endsWith(".v1");
                assertThat(envelope.occurredAt()).isEqualTo(OCCURRED_AT);
                assertThat(
                        registry.decode(
                                envelope.eventType(),
                                envelope.payload()
                        )
                ).isEqualTo(event);
            }
        }

        @Test
        @DisplayName("Alias는 Java class name과 분리된 계약이다")
        void usesStableAliases() {
            assertThat(registry.aliasFor(PlayerRegistered.class))
                    .isEqualTo("player.registered.v1");
            assertThat(registry.aliasFor(PlayerLeveledUp.class))
                    .isEqualTo("player.leveled-up.v1");
            assertThat(registry.aliasFor(LifeLogRecorded.class))
                    .isEqualTo("lifelog.recorded.v1");
            assertThat(registry.aliasFor(QuestEvent.class))
                    .isEqualTo("quest.event.v1");
            assertThat(registry.aliasFor(QuestRewardReadyFact.class))
                    .isEqualTo("quest.reward-ready.v1");
            assertThat(registry.aliasFor(EconomyEvent.class))
                    .isEqualTo("economy.event.v1");
        }

        @Test
        @DisplayName("QuestEvent의 Map 숫자·Instant와 Enum을 보존한다")
        void preservesQuestPayloadTypes() {
            QuestEvent source = questEvent();

            OutboxEventEnvelope envelope = registry.encode(source);
            QuestEvent decoded = (QuestEvent) registry.decode(
                    envelope.eventType(),
                    envelope.payload()
            );

            assertThat(decoded.type()).isEqualTo(QuestEventType.QUEST_PROGRESS);
            assertThat(decoded.attributes().get("progress"))
                    .isInstanceOf(Integer.class)
                    .isEqualTo(3);
            assertThat(decoded.attributes().get("reachedAt"))
                    .isInstanceOf(Instant.class)
                    .isEqualTo(OCCURRED_AT);
            assertThat(decoded.attributes().get("stats"))
                    .isEqualTo(Map.of("strength", 2, "luck", 1L));
        }

        @Test
        @DisplayName("null 속성은 순서와 함께 보존되고 복원된 Map은 수정할 수 없다")
        void preservesNullAttributeOrderAndImmutability() {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("first", 1);
            attributes.put("nullable", null);
            attributes.put("last", "end");
            EconomyEvent source = economyEvent(attributes);

            OutboxEventEnvelope envelope = registry.encode(source);
            EconomyEvent decoded = (EconomyEvent) registry.decode(
                    envelope.eventType(),
                    envelope.payload()
            );

            assertThat(decoded.attributes()).containsEntry("nullable", null);
            assertThat(new ArrayList<>(decoded.attributes().keySet()))
                    .containsExactly("first", "nullable", "last");
            assertThatThrownBy(() -> decoded.attributes().put("another", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("숫자와 Instant, Map, List 속성 타입은 왕복 후에도 유지된다")
        void preservesSupportedAttributeTypes() {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("byte", (byte) 1);
            attributes.put("short", (short) 2);
            attributes.put("integer", 3);
            attributes.put("long", 4L);
            attributes.put("float", 5.5F);
            attributes.put("double", 6.5D);
            attributes.put("bigInteger", new BigInteger("7000000000000000000"));
            attributes.put("bigDecimal", new BigDecimal("8.125"));
            attributes.put("instant", OCCURRED_AT);
            attributes.put("map", Map.of("nested", 9L));
            attributes.put("list", List.of("value", 10, OCCURRED_AT));
            EconomyEvent source = economyEvent(attributes);

            OutboxEventEnvelope envelope = registry.encode(source);
            EconomyEvent decoded = (EconomyEvent) registry.decode(
                    envelope.eventType(),
                    envelope.payload()
            );

            assertThat(decoded.attributes()).isEqualTo(attributes);
        }

        @Test
        @DisplayName("신규 QuestCompleted Definition Snapshot을 그대로 왕복한다")
        void roundTripsProfileQuestCompletedSnapshot() {
            QuestEvent source = completedQuestEvent(Map.of(
                    "acceptanceId", 19701L,
                    "questDefinitionVersion", 4,
                    "rewardProfileCode", "RP_EXP_30",
                    "completedAt", OCCURRED_AT
            ));

            OutboxEventEnvelope envelope = registry.encode(source);
            QuestEvent decoded = (QuestEvent) registry.decode(
                    envelope.eventType(),
                    envelope.payload()
            );

            assertThat(decoded).isEqualTo(source);
            assertThat(decoded.attributes())
                    .containsEntry("questDefinitionVersion", 4)
                    .containsEntry("rewardProfileCode", "RP_EXP_30")
                    .doesNotContainKeys(
                            "rewardExp",
                            "rewardStats",
                            "rewardLines",
                            "rewardProfileId"
                    );
        }

        @Test
        @DisplayName("legacy QuestCompleted는 rewardProfileCode 없이도 왕복한다")
        void roundTripsLegacyQuestCompletedWithoutProfileCode() {
            QuestEvent source = completedQuestEvent(Map.of(
                    "acceptanceId", 19702L,
                    "questDefinitionVersion", 1,
                    "completedAt", OCCURRED_AT
            ));

            OutboxEventEnvelope envelope = registry.encode(source);
            QuestEvent decoded = (QuestEvent) registry.decode(
                    envelope.eventType(),
                    envelope.payload()
            );

            assertThat(decoded).isEqualTo(source);
            assertThat(decoded.attributes())
                    .containsEntry("questDefinitionVersion", 1)
                    .doesNotContainKey("rewardProfileCode");
        }
    }

    @Nested
    @DisplayName("지원하지 않는 동적 속성 타입")
    class UnsupportedAttributeTypes {

        @Test
        @DisplayName("Character 속성은 append 전에 명시적으로 거부한다")
        void rejectsCharacterAttribute() {
            EconomyEvent event = economyEvent(Map.of("grade", 'A'));

            assertThatThrownBy(() -> registry.encode(event))
                    .isInstanceOfSatisfying(
                            DomainException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(OutboxError.OUTBOX_EVENT_ATTRIBUTE_TYPE_UNSUPPORTED)
                    );
        }

        @Test
        @DisplayName("Enum 속성은 append 전에 명시적으로 거부한다")
        void rejectsEnumAttribute() {
            EconomyEvent event = economyEvent(
                    Map.of("eventType", QuestEventType.QUEST_PROGRESS)
            );

            assertThatThrownBy(() -> registry.encode(event))
                    .isInstanceOfSatisfying(
                            DomainException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(OutboxError.OUTBOX_EVENT_ATTRIBUTE_TYPE_UNSUPPORTED)
                    );
        }
    }

    @Nested
    @DisplayName("등록되지 않은 Alias를 읽을 때")
    class UnknownAlias {

        @Test
        @DisplayName("안정된 OutboxError로 거부한다")
        void rejectsUnknownAlias() {
            assertThatThrownBy(() ->
                    registry.decode("unknown.event.v1", "{}")
            ).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(OutboxError.OUTBOX_EVENT_TYPE_UNKNOWN)
            );
        }
    }

    private QuestEvent questEvent() {
        return new QuestEvent(
                QuestEventType.QUEST_PROGRESS,
                197L,
                91L,
                "Q_OUTBOX",
                Map.of(
                        "progress", 3,
                        "reachedAt", OCCURRED_AT,
                        "stats", Map.of("strength", 2, "luck", 1L)
                ),
                OCCURRED_AT,
                "quest:197:progress"
        );
    }

    private QuestEvent completedQuestEvent(Map<String, Object> attributes) {
        return new QuestEvent(
                QuestEventType.QUEST_COMPLETED,
                197L,
                91L,
                "Q_OUTBOX",
                attributes,
                OCCURRED_AT,
                "quest:91:acceptance:197:completed"
        );
    }

    private QuestRewardReadyFact rewardReadyFact() {
        return new QuestRewardReadyFact(
                QuestRewardReadyFact.EVENT_VERSION,
                197L,
                19701L,
                "RP_EXP_30",
                91L,
                "Q_OUTBOX",
                4,
                OCCURRED_AT,
                "quest:91:acceptance:19701:completed:reward"
        );
    }

    private EconomyEvent economyEvent() {
        return economyEvent(Map.of(
                "price", 1_000L,
                "confirmedAt", OCCURRED_AT
        ));
    }

    private EconomyEvent economyEvent(Map<String, Object> attributes) {
        return new EconomyEvent(
                EconomyEventType.LISTING_PURCHASED,
                197L,
                101L,
                102L,
                null,
                null,
                "reservation",
                "economy:197",
                OCCURRED_AT,
                attributes
        );
    }
}
