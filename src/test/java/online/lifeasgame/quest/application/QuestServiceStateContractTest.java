package online.lifeasgame.quest.application;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.blueprint.StaticQuestBlueprintCatalog;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.event.QuestCompletionEventFactory;
import online.lifeasgame.quest.application.event.QuestDefinitionEventFactory;
import online.lifeasgame.quest.application.event.QuestTransitionEventFactory;
import online.lifeasgame.quest.application.query.QuestQuery;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
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
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestService 상태 계약")
class QuestServiceStateContractTest {

    private static final Long QUEST_ID = 193L;
    private static final Long ACCEPTANCE_ID = 1930L;
    private static final Long PLAYER_ID = 19300L;
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-30T01:00:00Z");
    private static final ZoneId PLAYER_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private QuestBlueprintCatalog questBlueprintCatalog;

    @Mock
    private QuestReader questReader;

    @Mock
    private QuestWriter questWriter;

    @Mock
    private RewardProfileLookupApi rewardProfileLookupApi;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private QuestService service;
    private QuestQueryService queryService;

    @BeforeEach
    void setUp() {
        service = new QuestService(
                mock(QuestDefinitionProvisioner.class),
                questReader,
                questWriter,
                rewardProfileLookupApi,
                domainEventPublisher,
                new QuestCompletionEventFactory(),
                new QuestDefinitionEventFactory(),
                new QuestTransitionEventFactory(),
                ignored -> PLAYER_ZONE,
                Clock.fixed(ACCEPTED_AT, ZoneOffset.UTC),
                mock(CurrentPlayerAccessor.class)
        );
        queryService = new QuestQueryService(
                questBlueprintCatalog,
                questReader,
                mock(CurrentPlayerAccessor.class)
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
            QuestEvent event = (QuestEvent) eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(QuestEventType.QUEST_COMPLETED);
            assertThat(event.attributes())
                    .containsEntry("questDefinitionVersion", 1)
                    .containsEntry("rewardExp", 45)
                    .containsEntry(
                            "rewardStats",
                            java.util.Map.of("wisdom", 3)
                    )
                    .doesNotContainKeys(
                            "rewardProfileCode",
                            "rewardLines",
                            "rewardProfileId"
                    );
            assertThat(event.correlationId())
                    .isEqualTo("quest:193:acceptance:1930:admin-completed");
        }
    }

    @Nested
    @DisplayName("Admin이 AUTO Quest 진행도를 목표까지 조정할 때")
    class AdjustAutoProgress {

        @Test
        @DisplayName("Factory canonical QUEST_COMPLETED를 admin-completed correlation으로 발행한다")
        void publishesCanonicalCompletion() {
            Quest quest = new StaticQuestBlueprintCatalog()
                    .require(QuestCode.Q_RECORD_FIRST_TRACE)
                    .instantiate();
            ReflectionTestUtils.setField(quest, "id", QUEST_ID);
            QuestAcceptance acceptance = QuestAcceptance.start(
                    QUEST_ID,
                    PLAYER_ID,
                    TimePeriod.forever(),
                    ACCEPTED_AT.minusSeconds(60),
                    null
            );
            ReflectionTestUtils.setField(
                    acceptance,
                    "id",
                    ACCEPTANCE_ID
            );
            given(questReader.getAcceptance(ACCEPTANCE_ID))
                    .willReturn(acceptance);
            given(questReader.getById(QUEST_ID)).willReturn(quest);

            QuestResult.Acceptance result =
                    service.adjustAcceptanceProgress(
                            ACCEPTANCE_ID,
                            new QuestCommand.AdjustProgress(1)
                    );

            assertThat(result.status())
                    .isEqualTo(QuestStatus.COMPLETED.name());
            ArgumentCaptor<DomainEvent> eventCaptor =
                    ArgumentCaptor.forClass(DomainEvent.class);
            verify(domainEventPublisher, times(3))
                    .publish(eventCaptor.capture());
            List<QuestEvent> events = eventCaptor.getAllValues().stream()
                    .map(QuestEvent.class::cast)
                    .toList();
            assertThat(events)
                    .extracting(QuestEvent::type)
                    .containsExactly(
                            QuestEventType.QUEST_PROGRESS,
                            QuestEventType.QUEST_GOAL_REACHED,
                            QuestEventType.QUEST_COMPLETED
                    );

            QuestEvent completed = events.get(2);
            assertThat(completed.questId()).isEqualTo(QUEST_ID);
            assertThat(completed.questCode())
                    .isEqualTo(QuestCode.Q_RECORD_FIRST_TRACE.value());
            assertThat(completed.playerId()).isEqualTo(PLAYER_ID);
            assertThat(completed.occurredAt()).isEqualTo(ACCEPTED_AT);
            assertThat(completed.attributes())
                    .containsEntry("acceptanceId", ACCEPTANCE_ID)
                    .containsEntry("progress", 1)
                    .containsEntry("target", 1)
                    .containsEntry("repeatRule", "ONCE")
                    .containsEntry("completionPolicy", "AUTO")
                    .containsEntry("goalReachedAt", ACCEPTED_AT)
                    .containsEntry("completedAt", ACCEPTED_AT)
                    .containsEntry("questDefinitionVersion", 1)
                    .containsEntry("questSemanticCategory", "RECORD")
                    .containsEntry("progressSource", "RECORD_CREATED")
                    .containsEntry("repeatPolicy", "ONCE")
                    .containsEntry(
                            "rewardProfileCode",
                            "RP_EXP_TINY_10"
                    )
                    .doesNotContainKeys(
                            "rewardExp",
                            "rewardStats",
                            "rewardLines",
                            "rewardProfileId"
                    );
            assertThat(completed.correlationId()).isEqualTo(
                    "quest:193:acceptance:1930:admin-completed"
            );
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

            queryService.questAcceptances(
                    new QuestQuery.Acceptances(QuestCode.PLAYER_WELCOME.name(), "DONE")
            );

            verify(questReader).findQuestAcceptances(QUEST_ID, QuestStatus.COMPLETED);
        }

        @Test
        @DisplayName("Player Quest 조회도 COMPLETED 조건으로 조회한다")
        void queriesPlayerAcceptancesAsCompleted() {
            given(questReader.findPlayerAcceptances(PLAYER_ID, QuestStatus.COMPLETED))
                    .willReturn(List.of());

            queryService.playerQuests(
                    PLAYER_ID,
                    new QuestQuery.PlayerQuests("DONE")
            );

            verify(questReader).findPlayerAcceptances(PLAYER_ID, QuestStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Player가 repeat 정책에 따라 Quest를 수락할 때")
    class AcceptByRepeatPolicy {

        @Test
        @DisplayName("ONCE는 영구 Acceptance 기간을 생성한다")
        void acceptsOnceWithForeverPeriod() {
            Quest quest = finalQuest(QuestRepeatRule.ONCE);
            given(questReader.getByCode(QuestCode.PLAYER_WELCOME))
                    .willReturn(quest);
            given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                    .willReturn(null);
            given(questWriter.saveAcceptance(any())).willAnswer(
                    invocation -> invocation.getArgument(0)
            );

            QuestResult.Acceptance result = service.accept(
                    PLAYER_ID,
                    new QuestCommand.Accept(
                            QuestCode.PLAYER_WELCOME.name(),
                            null,
                            null
                    )
            );

            assertThat(result.periodStart())
                    .isEqualTo(LocalDate.of(1970, 1, 1));
            assertThat(result.periodEnd())
                    .isEqualTo(LocalDate.of(9999, 12, 31));
            assertThat(result.repeatPolicy()).isEqualTo("ONCE");
        }

        @Test
        @DisplayName("DAILY는 이전 기간 완료 후 현재 기간 재수락을 허용한다")
        void acceptsNextDailyPeriod() {
            Quest quest = finalQuest(QuestRepeatRule.DAILY);
            LocalDate today = ACCEPTED_AT.atZone(PLAYER_ZONE).toLocalDate();
            QuestAcceptance previous = QuestAcceptance.start(
                    QUEST_ID,
                    PLAYER_ID,
                    TimePeriod.daily(today.minusDays(1)),
                    ACCEPTED_AT.minusSeconds(86_400),
                    null
            );
            previous.reachGoal(Instant.now().minusSeconds(60));
            previous.complete(Instant.now().minusSeconds(30));
            given(questReader.getByCode(QuestCode.PLAYER_WELCOME))
                    .willReturn(quest);
            given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                    .willReturn(previous);
            given(questWriter.saveAcceptance(any())).willAnswer(
                    invocation -> invocation.getArgument(0)
            );

            QuestResult.Acceptance result = service.accept(
                    PLAYER_ID,
                    new QuestCommand.Accept(
                            QuestCode.PLAYER_WELCOME.name(),
                            null,
                            null
                    )
            );

            assertThat(result.periodStart()).isEqualTo(today);
            assertThat(result.periodEnd()).isEqualTo(today);
        }

        @Test
        @DisplayName("ONCE의 기존 Acceptance가 있으면 재수락을 거부한다")
        void rejectsRepeatedOnceAcceptance() {
            Quest quest = finalQuest(QuestRepeatRule.ONCE);
            QuestAcceptance previous = QuestAcceptance.start(
                    QUEST_ID,
                    PLAYER_ID,
                    TimePeriod.forever(),
                    ACCEPTED_AT.minusSeconds(60),
                    null
            );
            given(questReader.getByCode(QuestCode.PLAYER_WELCOME))
                    .willReturn(quest);
            given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                    .willReturn(previous);

            assertThatThrownBy(() -> service.accept(
                    PLAYER_ID,
                    new QuestCommand.Accept(
                            QuestCode.PLAYER_WELCOME.name(),
                            null,
                            null
                    )
            ))
                    .isInstanceOfSatisfying(
                            DomainException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(
                                            QuestError
                                                    .QUEST_ACCEPTANCE_ALREADY_EXISTS
                                    )
                    );
        }

        @Test
        @DisplayName("같은 period의 CANCELED Acceptance는 기존 row에서 restart한다")
        void restartsCanceledAcceptanceInSamePeriod() {
            Quest quest = new StaticQuestBlueprintCatalog()
                    .require(QuestCode.Q_RECORD_WEEKLY_LOOKBACK)
                    .instantiate();
            ReflectionTestUtils.setField(quest, "id", QUEST_ID);
            LocalDate today = ACCEPTED_AT.atZone(PLAYER_ZONE).toLocalDate();
            QuestAcceptance previous = QuestAcceptance.start(
                    QUEST_ID,
                    PLAYER_ID,
                    1L,
                    2L,
                    TimePeriod.weekly(today),
                    ACCEPTED_AT.minusSeconds(60),
                    "2026-W30"
            );
            ReflectionTestUtils.setField(
                    previous,
                    "id",
                    ACCEPTANCE_ID
            );
            previous.setProgress(
                    1,
                    quest,
                    ACCEPTED_AT.minusSeconds(30)
            );
            previous.assignIdempotencyKey("old-attempt");
            previous.cancel();
            given(questReader.getByCode(
                    QuestCode.Q_RECORD_WEEKLY_LOOKBACK
            )).willReturn(quest);
            given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                    .willReturn(previous);
            given(questWriter.saveAcceptance(previous))
                    .willReturn(previous);

            QuestResult.Acceptance result = service.accept(
                    PLAYER_ID,
                    new QuestCommand.Accept(
                            QuestCode.Q_RECORD_WEEKLY_LOOKBACK.value(),
                            3L,
                            4L
                    )
            );

            assertThat(result.id()).isEqualTo(ACCEPTANCE_ID);
            assertThat(result.acceptedAt()).isEqualTo(ACCEPTED_AT);
            assertThat(result.periodKey()).isEqualTo("2026-W31");
            assertThat(result.status())
                    .isEqualTo(QuestStatus.IN_PROGRESS.name());
            assertThat(result.progressValue()).isZero();
            assertThat(result.goalReachedAt()).isNull();
            assertThat(result.completedAt()).isNull();
            assertThat(previous.getPartyId()).isEqualTo(3L);
            assertThat(previous.getGuildId()).isEqualTo(4L);
            assertThat(previous.getIdempotencyKey()).isNull();
            verify(questWriter).saveAcceptance(previous);
        }

        @Test
        @DisplayName("같은 period의 active, goal reached, completed Acceptance는 거부한다")
        void rejectsNonCanceledAcceptanceInSamePeriod() {
            Quest quest = finalQuest(QuestRepeatRule.ONCE);
            QuestAcceptance inProgress = QuestAcceptance.start(
                    QUEST_ID,
                    PLAYER_ID,
                    TimePeriod.forever(),
                    ACCEPTED_AT.minusSeconds(60),
                    null
            );
            QuestAcceptance goalReached = QuestAcceptance.start(
                    QUEST_ID,
                    PLAYER_ID,
                    TimePeriod.forever(),
                    ACCEPTED_AT.minusSeconds(60),
                    null
            );
            goalReached.reachGoal(ACCEPTED_AT.minusSeconds(30));
            QuestAcceptance completed = QuestAcceptance.start(
                    QUEST_ID,
                    PLAYER_ID,
                    TimePeriod.forever(),
                    ACCEPTED_AT.minusSeconds(60),
                    null
            );
            completed.reachGoal(ACCEPTED_AT.minusSeconds(30));
            completed.complete(ACCEPTED_AT.minusSeconds(20));
            given(questReader.getByCode(QuestCode.PLAYER_WELCOME))
                    .willReturn(quest);

            for (QuestAcceptance previous :
                    List.of(inProgress, goalReached, completed)) {
                given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                        .willReturn(previous);

                assertThatThrownBy(() -> service.accept(
                        PLAYER_ID,
                        new QuestCommand.Accept(
                                QuestCode.PLAYER_WELCOME.name(),
                                null,
                                null
                        )
                )).isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        QuestError
                                                .QUEST_ACCEPTANCE_ALREADY_EXISTS
                                )
                );
            }

            verifyNoInteractions(questWriter);
        }

        @Test
        @DisplayName("신규 Seed Quest를 nullable category와 공식 code로 수락한다")
        void acceptsSeedLevel1Quest() {
            Quest quest = new StaticQuestBlueprintCatalog()
                    .require(QuestCode.Q_RECORD_FIRST_TRACE)
                    .instantiate();
            ReflectionTestUtils.setField(quest, "id", QUEST_ID);
            given(questReader.getByCode(
                    QuestCode.Q_RECORD_FIRST_TRACE
            )).willReturn(quest);
            given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                    .willReturn(null);
            given(questWriter.saveAcceptance(any())).willAnswer(
                    invocation -> invocation.getArgument(0)
            );

            QuestResult.Acceptance result = service.accept(
                    PLAYER_ID,
                    new QuestCommand.Accept(
                            "Q_RECORD_FIRST_TRACE",
                            null,
                            null
                    )
            );

            assertThat(result.code()).isEqualTo("Q_RECORD_FIRST_TRACE");
            assertThat(result.category()).isNull();
            assertThat(result.semanticCategory()).isEqualTo("RECORD");
            assertThat(result.progressSource())
                    .isEqualTo("RECORD_CREATED");
            assertThat(result.targetType())
                    .isEqualTo(QuestTargetType.COUNT);
            assertThat(result.targetValue()).isEqualTo(1);
            assertThat(result.repeatPolicy()).isEqualTo("ONCE");
            assertThat(result.acceptedAt()).isEqualTo(ACCEPTED_AT);
            assertThat(result.periodKey()).isNull();
        }

        @Test
        @DisplayName("주간 기록 Quest는 Player timezone의 ISO periodKey를 저장한다")
        void acceptsWeeklyLookbackWithServerPeriodKey() {
            Quest quest = new StaticQuestBlueprintCatalog()
                    .require(QuestCode.Q_RECORD_WEEKLY_LOOKBACK)
                    .instantiate();
            ReflectionTestUtils.setField(quest, "id", QUEST_ID);
            given(questReader.getByCode(
                    QuestCode.Q_RECORD_WEEKLY_LOOKBACK
            )).willReturn(quest);
            given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                    .willReturn(null);
            given(questWriter.saveAcceptance(any())).willAnswer(
                    invocation -> invocation.getArgument(0)
            );

            QuestResult.Acceptance result = service.accept(
                    PLAYER_ID,
                    new QuestCommand.Accept(
                            "Q_RECORD_WEEKLY_LOOKBACK",
                            null,
                            null
                    )
            );

            assertThat(result.acceptedAt()).isEqualTo(ACCEPTED_AT);
            assertThat(result.periodKey()).isEqualTo("2026-W31");
            assertThat(result.periodStart())
                    .isEqualTo(LocalDate.of(2026, 7, 27));
            assertThat(result.periodEnd())
                    .isEqualTo(LocalDate.of(2026, 8, 2));
        }
    }

    private QuestAcceptance goalReachedAcceptance(Quest quest) {
        QuestAcceptance acceptance = QuestAcceptance.start(
                QUEST_ID,
                PLAYER_ID,
                TimePeriod.forever(),
                ACCEPTED_AT,
                null
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
                QuestReward.of(
                        45,
                        new RewardStats(java.util.Map.of("wisdom", 3))
                ),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.USER_CONFIRM,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }

    private Quest finalQuest(QuestRepeatRule repeatPolicy) {
        Quest quest = Quest.createDefinition(
                QuestCode.PLAYER_WELCOME.value(),
                2,
                QuestCategory.MAIN,
                QuestSemanticCategory.GROWTH,
                QuestTitle.of("Repeat 수락 계약"),
                "Repeat 수락 계약 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestProgressSource.COUNT,
                RewardProfileRef.of("RP_EXP_10"),
                repeatPolicy,
                null,
                QuestCompletionPolicy.USER_CONFIRM,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }
}
