package online.lifeasgame.quest.application.internal.event;

import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestRewardReadyFact provider contract")
class QuestRewardReadyFactTest {

    private static final Instant COMPLETED_AT =
            Instant.parse("2026-07-30T03:00:00Z");
    private static final Instant READY_AT =
            Instant.parse("2026-07-30T03:00:01Z");

    @Test
    @DisplayName("final Quest lifecycle snapshot을 typed fact로 변환한다")
    void createsFromFinalQuestSnapshot() {
        QuestRewardReadyFact fact = QuestRewardReadyFact.from(
                event(Map.of(
                        "acceptanceId", 21900L,
                        "rewardProfileCode", "  RP_EXP_TINY_10  ",
                        "questDefinitionVersion", 7
                )),
                READY_AT,
                "quest:219:completed:reward"
        ).orElseThrow();

        assertThat(fact.eventVersion())
                .isEqualTo(QuestRewardReadyFact.EVENT_VERSION);
        assertThat(fact.playerId()).isEqualTo(2190L);
        assertThat(fact.acceptanceId()).isEqualTo(21900L);
        assertThat(fact.rewardProfileCode()).isEqualTo("RP_EXP_TINY_10");
        assertThat(fact.questDefinitionVersion()).isEqualTo(7);
        assertThat(fact.occurredAt()).isEqualTo(READY_AT);
    }

    @Test
    @DisplayName("legacy inline reward snapshot은 typed reward fact를 만들지 않는다")
    void ignoresLegacyInlineRewardSnapshot() {
        assertThat(QuestRewardReadyFact.from(
                event(Map.of(
                        "acceptanceId", 21900L,
                        "questDefinitionVersion", 1,
                        "rewardExp", 10
                )),
                READY_AT,
                "legacy:reward"
        )).isEmpty();
    }

    @Test
    @DisplayName("final marker가 있는데 profile이 없으면 malformed event로 거부한다")
    void rejectsMissingFinalProfile() {
        assertThatThrownBy(() -> QuestRewardReadyFact.from(
                event(Map.of(
                        "acceptanceId", 21900L,
                        "questDefinitionVersion", 7,
                        "progressSource", "COUNT"
                )),
                READY_AT,
                "malformed:reward"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rewardProfileCode");
    }

    @Test
    @DisplayName("identity와 definition version은 양의 exact integer만 허용한다")
    void validatesExactIntegerIdentity() {
        for (Object invalid : new Object[]{
                0L,
                -1L,
                new BigDecimal("21900"),
                21900.0D,
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
        }) {
            assertThatThrownBy(() -> QuestRewardReadyFact.from(
                    event(Map.of(
                            "acceptanceId", invalid,
                            "rewardProfileCode", "RP_NONE",
                            "questDefinitionVersion", 1
                    )),
                    READY_AT,
                    "invalid:identity"
            )).isInstanceOf(IllegalArgumentException.class);
        }

        assertThatThrownBy(() -> QuestRewardReadyFact.from(
                event(Map.of(
                        "acceptanceId", 21900L,
                        "rewardProfileCode", "RP_NONE",
                        "questDefinitionVersion",
                        (long) Integer.MAX_VALUE + 1
                )),
                READY_AT,
                "invalid:version"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("questDefinitionVersion");
    }

    private QuestEvent event(Map<String, Object> attributes) {
        return new QuestEvent(
                QuestEventType.QUEST_COMPLETED,
                2190L,
                219L,
                "Q_FIRST_STEP",
                attributes,
                COMPLETED_AT,
                "quest:219:completed"
        );
    }
}
