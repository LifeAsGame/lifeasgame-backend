package online.lifeasgame.quest.application;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestService 상태 계약")
class QuestServiceStateContractTest {

    private static final Long QUEST_ID = 193L;
    private static final Long ACCEPTANCE_ID = 1930L;
    private static final Long PLAYER_ID = 19300L;

    @Mock
    private QuestBlueprintCatalog questBlueprintCatalog;

    @Mock
    private QuestReader questReader;

    @Mock
    private QuestWriter questWriter;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private QuestService service;

    @BeforeEach
    void setUp() {
        service = new QuestService(
                questBlueprintCatalog,
                questReader,
                questWriter,
                domainEventPublisher
        );
    }

    @Nested
    @DisplayName("Admin 상태 변경에 Legacy DONE을 입력할 때")
    class ChangeLegacyDone {

        @Test
        @DisplayName("GOAL_REACHED를 COMPLETED로 전이하고 Completed Event를 한 번 발행한다")
        void completesAsAlias() {
            Quest quest = quest();
            QuestAcceptance acceptance = goalReachedAcceptance(quest);
            given(questReader.getAcceptance(ACCEPTANCE_ID)).willReturn(acceptance);
            given(questReader.getById(QUEST_ID)).willReturn(quest);

            QuestResult.Acceptance result = service.changeAcceptanceStatus(
                    ACCEPTANCE_ID,
                    new QuestCommand.ChangeStatus("DONE", "legacy client")
            );

            assertThat(result.status()).isEqualTo(QuestStatus.COMPLETED.name());
            assertThat(result.status()).isNotEqualTo("DONE");

            ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
            verify(domainEventPublisher).publish(eventCaptor.capture());
            assertThat(((QuestEvent) eventCaptor.getValue()).type())
                    .isEqualTo(QuestEventType.QUEST_COMPLETED);
        }
    }

    @Nested
    @DisplayName("상태 Query에 Legacy DONE을 입력할 때")
    class QueryLegacyDone {

        @Test
        @DisplayName("Admin Acceptance 조회는 COMPLETED 조건으로 조회한다")
        void queriesAdminAcceptancesAsCompleted() {
            Quest quest = quest();
            given(questReader.getByCode(QuestCode.PLAYER_WELCOME)).willReturn(quest);
            given(questReader.findQuestAcceptances(QUEST_ID, QuestStatus.COMPLETED))
                    .willReturn(List.of());

            service.questAcceptances(
                    new QuestCommand.Acceptances(QuestCode.PLAYER_WELCOME.name(), "DONE")
            );

            verify(questReader).findQuestAcceptances(QUEST_ID, QuestStatus.COMPLETED);
        }

        @Test
        @DisplayName("Player Quest 조회도 COMPLETED 조건으로 조회한다")
        void queriesPlayerAcceptancesAsCompleted() {
            given(questReader.findPlayerAcceptances(PLAYER_ID, QuestStatus.COMPLETED))
                    .willReturn(List.of());

            service.playerQuests(PLAYER_ID, new QuestCommand.PlayerQuests("DONE"));

            verify(questReader).findPlayerAcceptances(PLAYER_ID, QuestStatus.COMPLETED);
        }
    }

    private QuestAcceptance goalReachedAcceptance(Quest quest) {
        QuestAcceptance acceptance = QuestAcceptance.start(
                QUEST_ID,
                PLAYER_ID,
                TimePeriod.forever()
        );
        ReflectionTestUtils.setField(acceptance, "id", ACCEPTANCE_ID);
        acceptance.setProgress(1, quest, Instant.parse("2026-07-23T03:00:00Z"));
        return acceptance;
    }

    private Quest quest() {
        Quest quest = Quest.create(
                QuestCode.PLAYER_WELCOME.value(),
                QuestCategory.MAIN,
                QuestTitle.of("서비스 상태 계약"),
                "QuestService 상태 계약 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.USER_CONFIRM,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }
}
