package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.character.domain.event.PlayerLeveledUp;
import online.lifeasgame.character.domain.event.PlayerRegistered;
import online.lifeasgame.inventory.domain.event.InventoryItemAdded;
import online.lifeasgame.lifelog.domain.event.CollectionLogged;
import online.lifeasgame.lifelog.domain.event.ExerciseLogged;
import online.lifeasgame.lifelog.domain.event.MediaLogAdvanced;
import online.lifeasgame.quest.application.automation.QuestSignal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Quest Trigger correlation")
class QuestTriggerCorrelationTest {

    private static final Long PLAYER_ID = 195L;
    private static final Instant FIRST_AT =
            Instant.parse("2026-07-24T01:00:00Z");
    private static final Instant SECOND_AT = FIRST_AT.plusSeconds(1);

    @Nested
    @DisplayName("같은 Source Event를 다시 번역할 때")
    class TranslateSameSourceEvent {

        @Test
        @DisplayName("모든 Trigger는 결정적인 필수 correlation을 생성한다")
        void createsStableCorrelations() {
            List<Translation> translations = List.of(
                    translation(
                            new CollectionLoggedQuestTrigger(),
                            new CollectionLogged(
                                    PLAYER_ID,
                                    1L,
                                    "BOOK",
                                    1,
                                    FIRST_AT
                            )
                    ),
                    translation(
                            new ExerciseLoggedQuestTrigger(),
                            new ExerciseLogged(
                                    PLAYER_ID,
                                    2L,
                                    "RUNNING",
                                    30,
                                    5.0,
                                    200,
                                    FIRST_AT
                            )
                    ),
                    translation(
                            new InventoryItemAddedQuestTrigger(),
                            new InventoryItemAdded(
                                    PLAYER_ID,
                                    3L,
                                    "COMMON",
                                    true,
                                    false,
                                    1,
                                    FIRST_AT
                            )
                    ),
                    translation(
                            new MediaLogAdvancedQuestTrigger(),
                            new MediaLogAdvanced(
                                    PLAYER_ID,
                                    4L,
                                    1,
                                    2,
                                    12,
                                    FIRST_AT
                            )
                    ),
                    translation(
                            new PlayerLeveledUpQuestTrigger(),
                            new PlayerLeveledUp(
                                    PLAYER_ID,
                                    9,
                                    10,
                                    FIRST_AT
                            )
                    ),
                    translation(
                            new PlayerRegisteredQuestTrigger(),
                            new PlayerRegistered(PLAYER_ID, FIRST_AT)
                    )
            );

            translations.forEach(translation -> {
                assertThat(translation.first())
                        .extracting(QuestSignal::correlationId)
                        .doesNotContainNull()
                        .allSatisfy(correlation -> {
                            assertThat(correlation).isNotBlank();
                            assertThat(correlation.length())
                                    .isLessThanOrEqualTo(
                                            QuestSignal.MAX_CORRELATION_ID_LENGTH
                                    );
                        });
                assertThat(translation.first())
                        .extracting(QuestSignal::correlationId)
                        .containsExactlyElementsOf(
                                translation.second().stream()
                                        .map(QuestSignal::correlationId)
                                        .toList()
                        );
            });
        }
    }

    @Nested
    @DisplayName("같은 Aggregate에서 반복 가능한 Event가 발생할 때")
    class DistinguishRepeatedSourceEvents {

        @Test
        @DisplayName("Inventory와 Media의 발생 시각이 다르면 다른 correlation을 만든다")
        void distinguishesRepeatedEvents() {
            InventoryItemAddedQuestTrigger inventoryTrigger =
                    new InventoryItemAddedQuestTrigger();
            MediaLogAdvancedQuestTrigger mediaTrigger =
                    new MediaLogAdvancedQuestTrigger();

            String firstInventory = inventoryTrigger.translate(
                    new InventoryItemAdded(
                            PLAYER_ID,
                            3L,
                            "COMMON",
                            true,
                            false,
                            1,
                            FIRST_AT
                    )
            ).getFirst().correlationId();
            String secondInventory = inventoryTrigger.translate(
                    new InventoryItemAdded(
                            PLAYER_ID,
                            3L,
                            "COMMON",
                            true,
                            false,
                            1,
                            SECOND_AT
                    )
            ).getFirst().correlationId();
            String firstMedia = mediaTrigger.translate(
                    new MediaLogAdvanced(
                            PLAYER_ID,
                            4L,
                            1,
                            2,
                            12,
                            FIRST_AT
                    )
            ).getFirst().correlationId();
            String secondMedia = mediaTrigger.translate(
                    new MediaLogAdvanced(
                            PLAYER_ID,
                            4L,
                            1,
                            3,
                            12,
                            SECOND_AT
                    )
            ).getFirst().correlationId();

            assertThat(firstInventory).isNotEqualTo(secondInventory);
            assertThat(firstMedia).isNotEqualTo(secondMedia);
        }

        @Test
        @DisplayName("같은 Level에 다시 도달해도 Source Event 시각으로 구분한다")
        void distinguishesRepeatedLevelEvents() {
            PlayerLeveledUpQuestTrigger trigger =
                    new PlayerLeveledUpQuestTrigger();

            String first = trigger.translate(
                    new PlayerLeveledUp(PLAYER_ID, 9, 10, FIRST_AT)
            ).getFirst().correlationId();
            String second = trigger.translate(
                    new PlayerLeveledUp(PLAYER_ID, 9, 10, SECOND_AT)
            ).getFirst().correlationId();

            assertThat(first).isNotEqualTo(second);
        }
    }

    private <T extends online.lifeasgame.core.event.DomainEvent> Translation translation(
            QuestTrigger<T> trigger,
            T event
    ) {
        return new Translation(
                List.copyOf(trigger.translate(event)),
                List.copyOf(trigger.translate(event))
        );
    }

    private record Translation(
            List<QuestSignal> first,
            List<QuestSignal> second
    ) {
    }
}
