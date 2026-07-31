package online.lifeasgame.quest.application.event;

import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.event.QuestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestCompletionEventFactory")
class QuestCompletionEventFactoryTest {

    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-30T01:00:00Z");
    private static final Instant GOAL_REACHED_AT =
            Instant.parse("2026-07-30T02:00:00Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-07-30T03:00:00Z");
    private static final Set<String> CANONICAL_ATTRIBUTES = Set.of(
            "acceptanceId",
            "progress",
            "target",
            "repeatRule",
            "completionPolicy",
            "goalReachedAt",
            "completedAt",
            "questDefinitionVersion",
            "questSemanticCategory",
            "progressSource",
            "repeatPolicy",
            "roleTemplateCode",
            "rewardProfileCode"
    );

    private final QuestCompletionEventFactory factory =
            new QuestCompletionEventFactory();
    private Quest quest;
    private QuestAcceptance acceptance;

    @BeforeEach
    void setUp() {
        quest = Quest.createDefinition(
                "quest:test:completion-factory",
                7,
                QuestSemanticCategory.GROWTH,
                QuestTitle.of("Factory"),
                "Factory test",
                QuestTarget.of(QuestTargetType.COUNT, 3),
                QuestProgressSource.COUNT,
                RewardProfileRef.of("RP_EXP_TINY_10"),
                QuestRepeatRule.ONCE,
                QuestRoleTemplateRef.of("ROLE_WARRIOR"),
                QuestCompletionPolicy.AUTO,
                null
        );
        ReflectionTestUtils.setField(quest, "id", 219L);
        acceptance = QuestAcceptance.start(
                219L,
                2190L,
                TimePeriod.forever(),
                ACCEPTED_AT,
                null
        );
        ReflectionTestUtils.setField(acceptance, "id", 21900L);
        acceptance.setProgress(3, quest, GOAL_REACHED_AT);
        acceptance.complete(COMPLETED_AT);
    }

    @Test
    @DisplayName("source context를 보존하고 canonical 완료 값과 보상 snapshot으로 위조 값을 덮어쓴다")
    void createsCanonicalPayloadWithoutContextOverride() {
        QuestEvent event = factory.create(
                acceptance,
                quest,
                "source:219:completed",
                Map.ofEntries(
                        Map.entry("lifeLogId", 901L),
                        Map.entry("acceptanceId", -1L),
                        Map.entry("progress", 999),
                        Map.entry("target", 999),
                        Map.entry("repeatRule", "DAILY"),
                        Map.entry("completionPolicy", "USER_CONFIRM"),
                        Map.entry("goalReachedAt", ACCEPTED_AT),
                        Map.entry("completedAt", ACCEPTED_AT),
                        Map.entry("questDefinitionVersion", 99),
                        Map.entry("rewardProfileCode", "RP_NONE"),
                        Map.entry("rewardExp", 999),
                        Map.entry("rewardStats", Map.of("luck", 999))
                )
        );

        assertThat(event.questId()).isEqualTo(219L);
        assertThat(event.questCode())
                .isEqualTo("quest:test:completion-factory");
        assertThat(event.playerId()).isEqualTo(2190L);
        assertThat(event.occurredAt()).isEqualTo(COMPLETED_AT);
        assertThat(event.correlationId()).isEqualTo("source:219:completed");
        assertThat(event.attributes())
                .containsEntry("lifeLogId", 901L)
                .containsEntry("acceptanceId", 21900L)
                .containsEntry("progress", 3)
                .containsEntry("target", 3)
                .containsEntry("repeatRule", "ONCE")
                .containsEntry("completionPolicy", "AUTO")
                .containsEntry("goalReachedAt", GOAL_REACHED_AT)
                .containsEntry("completedAt", COMPLETED_AT)
                .containsEntry("questDefinitionVersion", 7)
                .containsEntry("questSemanticCategory", "GROWTH")
                .containsEntry("progressSource", "COUNT")
                .containsEntry("repeatPolicy", "ONCE")
                .containsEntry("roleTemplateCode", "ROLE_WARRIOR")
                .containsEntry("rewardProfileCode", "RP_EXP_TINY_10")
                .doesNotContainKeys(
                        "rewardExp",
                        "rewardStats",
                        "rewardLines",
                        "rewardProfileId"
                );
    }

    @Test
    @DisplayName("USER_CONFIRM과 AUTO 조립은 correlation/source context 외 canonical 값이 같다")
    void keepsCanonicalParityAcrossCompletionPaths() {
        QuestEvent userConfirm = factory.create(
                acceptance,
                quest,
                "quest:219:acceptance:21900:completed"
        );
        QuestEvent auto = factory.create(
                acceptance,
                quest,
                "lifelog:901:completed",
                Map.of("lifeLogId", 901L)
        );

        assertThat(canonical(auto)).isEqualTo(canonical(userConfirm));
        assertThat(auto.questId()).isEqualTo(userConfirm.questId());
        assertThat(auto.questCode()).isEqualTo(userConfirm.questCode());
        assertThat(auto.playerId()).isEqualTo(userConfirm.playerId());
        assertThat(auto.occurredAt()).isEqualTo(userConfirm.occurredAt());
        assertThat(userConfirm.attributes()).doesNotContainKey("lifeLogId");
        assertThat(auto.attributes()).containsEntry("lifeLogId", 901L);
        assertThat(auto.correlationId())
                .isNotEqualTo(userConfirm.correlationId());
    }

    @Test
    @DisplayName("null, 미영속, 미완료, blank correlation 입력은 raw NPE 없이 거부한다")
    void validatesCompletionPrerequisites() {
        assertThatThrownBy(() ->
                factory.create(null, quest, "correlation")
        ).isInstanceOf(IllegalArgumentException.class);

        ReflectionTestUtils.setField(acceptance, "id", null);
        assertThatThrownBy(() ->
                factory.create(acceptance, quest, "correlation")
        ).isInstanceOf(IllegalArgumentException.class);
        ReflectionTestUtils.setField(acceptance, "id", 21900L);

        QuestAcceptance inProgress = QuestAcceptance.start(
                219L,
                2190L,
                TimePeriod.forever(),
                ACCEPTED_AT,
                null
        );
        ReflectionTestUtils.setField(inProgress, "id", 21901L);
        assertThatThrownBy(() ->
                factory.create(inProgress, quest, "correlation")
        ).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                factory.create(acceptance, quest, " ")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private Map<String, Object> canonical(QuestEvent event) {
        return event.attributes().entrySet().stream()
                .filter(entry -> CANONICAL_ATTRIBUTES.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}
