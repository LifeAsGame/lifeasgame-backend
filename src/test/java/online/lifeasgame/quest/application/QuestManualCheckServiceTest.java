package online.lifeasgame.quest.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.application.automation.QuestSignalAcceptancePolicy;
import online.lifeasgame.quest.application.automation.QuestSignalProcessingService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.error.QuestError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestManualCheckService")
class QuestManualCheckServiceTest {

    private static final Long PLAYER_ID = 217L;
    private static final Long QUEST_ID = 2170L;
    private static final Long ACCEPTANCE_ID = 21700L;
    private static final Instant CHECKED_AT =
            Instant.parse("2026-07-31T03:00:00Z");
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-31T01:00:00Z");
    private static final ZoneId PLAYER_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private QuestReader questReader;

    @Mock
    private QuestSignalProcessingService signalProcessingService;

    @Mock
    private QuestAcceptanceCompletionService completionService;

    @Mock
    private PlayerTimezoneResolver playerTimezoneResolver;

    private QuestManualCheckService service;

    @BeforeEach
    void setUp() {
        service = new QuestManualCheckService(
                questReader,
                signalProcessingService,
                completionService,
                playerTimezoneResolver,
                Clock.fixed(CHECKED_AT, ZoneId.of("UTC"))
        );
        lenient().when(playerTimezoneResolver.resolve(PLAYER_ID))
                .thenReturn(PLAYER_ZONE);
    }

    @ParameterizedTest
    @CsvSource({
            "Q_GROWTH_ONE_FOCUS,25",
            "Q_RECOVERY_REST_TEN,10"
    })
    @DisplayName("허용 Quest target을 SET하고 현재 Acceptance를 완료한다")
    void setsTargetAndCompletes(String rawCode, int target) {
        QuestCode code = QuestCode.parse(rawCode);
        Quest quest = manualQuest(code, target);
        QuestAcceptance inProgress = acceptance(quest, PLAYER_ID, ACCEPTED_AT);
        ReflectionTestUtils.setField(
                inProgress,
                "periodKey",
                "2026-W31"
        );
        QuestAcceptance goalReached = acceptance(
                quest,
                PLAYER_ID,
                ACCEPTED_AT
        );
        ReflectionTestUtils.setField(
                goalReached,
                "periodKey",
                "2026-W31"
        );
        goalReached.setProgress(target, quest, CHECKED_AT);
        QuestResult.Acceptance completed =
                QuestResult.Acceptance.from(goalReached, quest);

        given(questReader.getByCode(code)).willReturn(quest);
        given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                .willReturn(inProgress, goalReached);
        given(completionService.completeForPlayer(
                PLAYER_ID,
                ACCEPTANCE_ID
        )).willReturn(completed);

        QuestResult.Acceptance result = service.check(
                PLAYER_ID,
                new QuestCommand.ManualCheck(rawCode)
        );

        assertThat(result).isSameAs(completed);
        ArgumentCaptor<QuestSignal> signalCaptor =
                ArgumentCaptor.forClass(QuestSignal.class);
        verify(signalProcessingService).process(signalCaptor.capture());
        QuestSignal signal = signalCaptor.getValue();
        assertThat(signal.questCode()).isEqualTo(code);
        assertThat(signal.playerId()).isEqualTo(PLAYER_ID);
        assertThat(signal.isSetOperation()).isTrue();
        assertThat(signal.progressValue()).isEqualTo(target);
        assertThat(signal.acceptancePolicy())
                .isEqualTo(QuestSignalAcceptancePolicy.EXISTING_ONLY);
        assertThat(signal.occurredAt()).isEqualTo(CHECKED_AT);
        assertThat(signal.periodKey()).isEqualTo("2026-W31");
        assertThat(signal.correlationId()).isEqualTo(
                "manual-check:acceptance:21700:accepted-at:"
                        + ACCEPTED_AT.toEpochMilli()
        );
        assertThat(signal.attributes())
                .containsEntry("acceptanceId", ACCEPTANCE_ID)
                .containsEntry(
                        "acceptanceAttemptId",
                        ACCEPTANCE_ID
                )
                .containsEntry(
                        "acceptanceAttemptAcceptedAt",
                        ACCEPTED_AT.toString()
                )
                .containsEntry("manualCheck", true)
                .containsEntry("source", "USER_CONFIRMATION");
        verify(completionService).completeForPlayer(
                PLAYER_ID,
                ACCEPTANCE_ID
        );
    }

    @Test
    @DisplayName("GOAL_REACHED는 Signal 없이 completion만 재시도한다")
    void completesGoalReachedWithoutSignal() {
        Quest quest = manualQuest(QuestCode.Q_GROWTH_ONE_FOCUS, 25);
        QuestAcceptance acceptance = acceptance(
                quest,
                PLAYER_ID,
                ACCEPTED_AT
        );
        acceptance.setProgress(25, quest, CHECKED_AT);
        QuestResult.Acceptance expected =
                QuestResult.Acceptance.from(acceptance, quest);
        given(questReader.getByCode(QuestCode.Q_GROWTH_ONE_FOCUS))
                .willReturn(quest);
        given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                .willReturn(acceptance);
        given(completionService.completeForPlayer(
                PLAYER_ID,
                ACCEPTANCE_ID
        )).willReturn(expected);

        assertThat(service.check(
                PLAYER_ID,
                new QuestCommand.ManualCheck("Q_GROWTH_ONE_FOCUS")
        )).isSameAs(expected);

        verifyNoInteractions(signalProcessingService);
    }

    @Test
    @DisplayName("COMPLETED는 lock 기반 completion no-op 결과를 반환한다")
    void returnsCompletedNoOp() {
        Quest quest = manualQuest(QuestCode.Q_RECOVERY_REST_TEN, 10);
        QuestAcceptance acceptance = acceptance(
                quest,
                PLAYER_ID,
                ACCEPTED_AT
        );
        acceptance.setProgress(10, quest, CHECKED_AT);
        acceptance.complete(CHECKED_AT.plusSeconds(1));
        QuestResult.Acceptance expected =
                QuestResult.Acceptance.from(acceptance, quest);
        given(questReader.getByCode(QuestCode.Q_RECOVERY_REST_TEN))
                .willReturn(quest);
        given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                .willReturn(acceptance);
        given(completionService.completeForPlayer(
                PLAYER_ID,
                ACCEPTANCE_ID
        )).willReturn(expected);

        QuestResult.Acceptance result = service.check(
                PLAYER_ID,
                new QuestCommand.ManualCheck("Q_RECOVERY_REST_TEN")
        );

        assertThat(result.completedAt()).isEqualTo(
                CHECKED_AT.plusSeconds(1)
        );
        verifyNoInteractions(signalProcessingService);
    }

    @Test
    @DisplayName("CANCELED Acceptance는 restart하지 않고 거부한다")
    void rejectsCanceled() {
        Quest quest = manualQuest(QuestCode.Q_GROWTH_ONE_FOCUS, 25);
        QuestAcceptance acceptance = acceptance(
                quest,
                PLAYER_ID,
                ACCEPTED_AT
        );
        acceptance.cancel();
        given(questReader.getByCode(QuestCode.Q_GROWTH_ONE_FOCUS))
                .willReturn(quest);
        given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                .willReturn(acceptance);

        assertError(
                () -> service.check(
                        PLAYER_ID,
                        new QuestCommand.ManualCheck(
                                "Q_GROWTH_ONE_FOCUS"
                        )
                ),
                QuestError.QUEST_MANUAL_CHECK_NOT_ALLOWED
        );
        verifyNoInteractions(
                signalProcessingService,
                completionService
        );
    }

    @Test
    @DisplayName("Acceptance가 없으면 auto-create하지 않는다")
    void rejectsMissingAcceptance() {
        Quest quest = manualQuest(QuestCode.Q_GROWTH_ONE_FOCUS, 25);
        given(questReader.getByCode(QuestCode.Q_GROWTH_ONE_FOCUS))
                .willReturn(quest);
        given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                .willReturn(null);

        assertError(
                () -> service.check(
                        PLAYER_ID,
                        new QuestCommand.ManualCheck(
                                "Q_GROWTH_ONE_FOCUS"
                        )
                ),
                QuestError.QUEST_ACCEPTANCE_NOT_FOUND
        );
        verifyNoInteractions(
                signalProcessingService,
                completionService
        );
    }

    @Test
    @DisplayName("이전 local DAILY period Acceptance는 거부한다")
    void rejectsOldPeriod() {
        Quest quest = manualQuest(QuestCode.Q_GROWTH_ONE_FOCUS, 25);
        QuestAcceptance acceptance = QuestAcceptance.start(
                QUEST_ID,
                PLAYER_ID,
                TimePeriod.daily(LocalDate.of(2026, 7, 30)),
                ACCEPTED_AT.minusSeconds(86400),
                null
        );
        ReflectionTestUtils.setField(
                acceptance,
                "id",
                ACCEPTANCE_ID
        );
        given(questReader.getByCode(QuestCode.Q_GROWTH_ONE_FOCUS))
                .willReturn(quest);
        given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                .willReturn(acceptance);

        assertError(
                () -> service.check(
                        PLAYER_ID,
                        new QuestCommand.ManualCheck(
                                "Q_GROWTH_ONE_FOCUS"
                        )
                ),
                QuestError.QUEST_ACCEPTANCE_NOT_FOUND
        );
    }

    @Test
    @DisplayName("다른 Player Acceptance는 not found로 숨긴다")
    void rejectsOtherPlayer() {
        Quest quest = manualQuest(QuestCode.Q_GROWTH_ONE_FOCUS, 25);
        QuestAcceptance acceptance = acceptance(
                quest,
                999L,
                ACCEPTED_AT
        );
        given(questReader.getByCode(QuestCode.Q_GROWTH_ONE_FOCUS))
                .willReturn(quest);
        given(questReader.findLatest(QUEST_ID, PLAYER_ID))
                .willReturn(acceptance);

        assertError(
                () -> service.check(
                        PLAYER_ID,
                        new QuestCommand.ManualCheck(
                                "Q_GROWTH_ONE_FOCUS"
                        )
                ),
                QuestError.QUEST_ACCEPTANCE_NOT_FOUND
        );
    }

    @Test
    @DisplayName("allowlist 밖 Quest는 안정된 409로 거부한다")
    void rejectsOtherQuest() {
        QuestCode code = QuestCode.Q_RECORD_FIRST_TRACE;
        Quest quest = manualQuest(code, 1);
        given(questReader.getByCode(code)).willReturn(quest);

        assertError(
                () -> service.check(
                        PLAYER_ID,
                        new QuestCommand.ManualCheck(code.value())
                ),
                QuestError.QUEST_MANUAL_CHECK_NOT_ALLOWED
        );
    }

    @Test
    @DisplayName("허용 code라도 runtime Definition이 다르면 거부한다")
    void rejectsWrongDefinition() {
        Quest quest = Quest.createDefinition(
                QuestCode.Q_GROWTH_ONE_FOCUS.value(),
                1,
                QuestSemanticCategory.GROWTH,
                QuestTitle.of("잘못된 수동 퀘스트"),
                "wrong source",
                QuestTarget.of(QuestTargetType.MINUTES, 25),
                QuestProgressSource.COUNT,
                RewardProfileRef.of("RP_NONE"),
                QuestRepeatRule.DAILY,
                null,
                QuestCompletionPolicy.USER_CONFIRM,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        given(questReader.getByCode(QuestCode.Q_GROWTH_ONE_FOCUS))
                .willReturn(quest);

        assertError(
                () -> service.check(
                        PLAYER_ID,
                        new QuestCommand.ManualCheck(
                                "Q_GROWTH_ONE_FOCUS"
                        )
                ),
                QuestError.QUEST_MANUAL_CHECK_NOT_ALLOWED
        );
    }

    private Quest manualQuest(QuestCode code, int target) {
        Quest quest = Quest.createDefinition(
                code.value(),
                1,
                QuestSemanticCategory.GROWTH,
                QuestTitle.of("수동 확인 퀘스트"),
                "manual check",
                QuestTarget.of(QuestTargetType.MINUTES, target),
                QuestProgressSource.MANUAL_CHECK,
                RewardProfileRef.of("RP_NONE"),
                QuestRepeatRule.DAILY,
                null,
                QuestCompletionPolicy.USER_CONFIRM,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }

    private QuestAcceptance acceptance(
            Quest quest,
            Long playerId,
            Instant acceptedAt
    ) {
        QuestAcceptance acceptance = QuestAcceptance.start(
                quest.getId(),
                playerId,
                TimePeriod.daily(LocalDate.of(2026, 7, 31)),
                acceptedAt,
                null
        );
        ReflectionTestUtils.setField(
                acceptance,
                "id",
                ACCEPTANCE_ID
        );
        return acceptance;
    }

    private void assertError(Runnable action, QuestError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
