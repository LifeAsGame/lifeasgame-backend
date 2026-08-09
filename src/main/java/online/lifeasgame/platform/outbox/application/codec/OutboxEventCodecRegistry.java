package online.lifeasgame.platform.outbox.application.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.character.domain.event.PlayerLeveledUp;
import online.lifeasgame.character.domain.event.PlayerRegistered;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.inventory.domain.event.InventoryItemAdded;
import online.lifeasgame.lifelog.domain.event.CollectionLogged;
import online.lifeasgame.lifelog.domain.event.ExerciseLogged;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.MediaLogAdvanced;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.social.domain.event.ChatChannelDeactivated;
import online.lifeasgame.user.domain.event.UserRegistered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutboxEventCodecRegistry {

    private final Map<String, OutboxEventCodec<?>> codecsByAlias;
    private final Map<Class<? extends DomainEvent>, OutboxEventCodec<?>>
            codecsByType;

    @Autowired
    public OutboxEventCodecRegistry(ObjectMapper objectMapper) {
        this(registeredCodecs(
                objectMapper.copy().deactivateDefaultTyping()
        ));
    }

    private static List<OutboxEventCodec<?>> registeredCodecs(
            ObjectMapper objectMapper
    ) {
        return List.of(
                codec(
                        "player.registered.v1",
                        PlayerRegistered.class,
                        objectMapper
                ),
                codec(
                        "player.leveled-up.v1",
                        PlayerLeveledUp.class,
                        objectMapper
                ),
                codec(
                        "inventory.item-added.v1",
                        InventoryItemAdded.class,
                        objectMapper
                ),
                codec(
                        "lifelog.collection-logged.v1",
                        CollectionLogged.class,
                        objectMapper
                ),
                codec(
                        "lifelog.exercise-logged.v1",
                        ExerciseLogged.class,
                        objectMapper
                ),
                new LifeLogRecordedOutboxCodec(objectMapper),
                codec(
                        "lifelog.media-advanced.v1",
                        MediaLogAdvanced.class,
                        objectMapper
                ),
                codec(
                        "user.registered.v1",
                        UserRegistered.class,
                        objectMapper
                ),
                codec(
                        "social.chat-channel-deactivated.v1",
                        ChatChannelDeactivated.class,
                        objectMapper
                ),
                new QuestEventOutboxCodec(objectMapper),
                codec(
                        "quest.reward-ready.v1",
                        QuestRewardReadyFact.class,
                        objectMapper
                ),
                new EconomyEventOutboxCodec(objectMapper)
        );
    }

    OutboxEventCodecRegistry(List<OutboxEventCodec<?>> codecs) {
        Map<String, OutboxEventCodec<?>> aliases = new LinkedHashMap<>();
        Map<Class<? extends DomainEvent>, OutboxEventCodec<?>> types =
                new LinkedHashMap<>();
        for (OutboxEventCodec<?> codec : codecs) {
            if (aliases.put(codec.alias(), codec) != null
                    || types.put(codec.eventType(), codec) != null) {
                throw new IllegalStateException(
                        "Duplicate outbox event codec registration"
                );
            }
        }
        codecsByAlias = Map.copyOf(aliases);
        codecsByType = Map.copyOf(types);
    }

    public OutboxEventEnvelope encode(DomainEvent event) {
        OutboxEventCodec<DomainEvent> codec = codecFor(event.getClass());
        return new OutboxEventEnvelope(
                codec.alias(),
                codec.encode(event),
                event.occurredAt()
        );
    }

    public DomainEvent decode(String alias, String payload) {
        OutboxEventCodec<?> codec = codecsByAlias.get(alias);
        if (codec == null) {
            throw new DomainException(
                    OutboxError.OUTBOX_EVENT_TYPE_UNKNOWN,
                    alias
            );
        }
        return codec.decode(payload);
    }

    public String aliasFor(Class<? extends DomainEvent> eventType) {
        return codecFor(eventType).alias();
    }

    public int size() {
        return codecsByAlias.size();
    }

    @SuppressWarnings("unchecked")
    private OutboxEventCodec<DomainEvent> codecFor(
            Class<? extends DomainEvent> eventType
    ) {
        OutboxEventCodec<?> codec = codecsByType.get(eventType);
        if (codec == null) {
            throw new DomainException(
                    OutboxError.OUTBOX_EVENT_TYPE_UNKNOWN,
                    eventType.getSimpleName()
            );
        }
        return (OutboxEventCodec<DomainEvent>) codec;
    }

    private static <T extends DomainEvent> OutboxEventCodec<T> codec(
            String alias,
            Class<T> eventType,
            ObjectMapper objectMapper
    ) {
        return new JacksonOutboxEventCodec<>(
                alias,
                eventType,
                objectMapper
        );
    }
}
