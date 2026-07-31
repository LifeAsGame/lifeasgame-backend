package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.application.automation.QuestSignalAcceptancePolicy;
import online.lifeasgame.quest.application.automation.QuestSignalProcessingService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.error.QuestError;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestManualCheckService {

    private static final Set<QuestCode> ALLOWED_CODES = Set.of(
            QuestCode.Q_GROWTH_ONE_FOCUS,
            QuestCode.Q_RECOVERY_REST_TEN
    );

    private final QuestReader questReader;
    private final QuestSignalProcessingService signalProcessingService;
    private final QuestAcceptanceCompletionService completionService;
    private final PlayerTimezoneResolver playerTimezoneResolver;
    private final Clock clock;

    public QuestResult.Acceptance check(
            Long playerId,
            QuestCommand.ManualCheck command
    ) {
        QuestCode questCode = QuestCode.parse(command.questCode());
        Quest quest = questReader.getByCode(questCode);
        assertManualCheckDefinition(questCode, quest);

        Instant checkedAt = clock.instant();
        ZoneId playerZone = Objects.requireNonNull(
                playerTimezoneResolver.resolve(playerId),
                "playerTimezone"
        );
        LocalDate currentDate = checkedAt.atZone(playerZone).toLocalDate();
        QuestAcceptance acceptance = questReader.findLatest(
                quest.getId(),
                playerId
        );
        assertCurrentAcceptance(
                acceptance,
                quest.getId(),
                playerId,
                currentDate
        );

        if (acceptance.isCanceled()) {
            throw new DomainException(
                    QuestError.QUEST_MANUAL_CHECK_NOT_ALLOWED
            );
        }
        if (acceptance.isInProgress()) {
            signalProcessingService.process(
                    manualCheckSignal(
                            questCode,
                            quest,
                            acceptance,
                            playerId,
                            checkedAt
                    )
            );
            QuestAcceptance refreshed = questReader.findLatest(
                    quest.getId(),
                    playerId
            );
            assertSameAttempt(acceptance, refreshed);
            acceptance = refreshed;
        }

        if (!acceptance.isGoalReached() && !acceptance.isCompleted()) {
            throw new DomainException(
                    QuestError.QUEST_MANUAL_CHECK_NOT_ALLOWED
            );
        }
        return completionService.completeForPlayer(
                playerId,
                acceptance.getId()
        );
    }

    private void assertManualCheckDefinition(
            QuestCode questCode,
            Quest quest
    ) {
        if (!ALLOWED_CODES.contains(questCode)
                || quest.getProgressSource()
                != QuestProgressSource.MANUAL_CHECK
                || !quest.requiresUserConfirmation()
                || quest.target().type() != QuestTargetType.MINUTES
                || quest.getRepeatRule() != QuestRepeatRule.DAILY) {
            throw new DomainException(
                    QuestError.QUEST_MANUAL_CHECK_NOT_ALLOWED
            );
        }
    }

    private void assertCurrentAcceptance(
            QuestAcceptance acceptance,
            Long questId,
            Long playerId,
            LocalDate currentDate
    ) {
        if (acceptance == null
                || !questId.equals(acceptance.getQuestId())
                || !playerId.equals(acceptance.getPlayerId())
                || !acceptance.getPeriod().contains(currentDate)) {
            throw new DomainException(
                    QuestError.QUEST_ACCEPTANCE_NOT_FOUND
            );
        }
    }

    private void assertSameAttempt(
            QuestAcceptance expected,
            QuestAcceptance actual
    ) {
        if (actual == null
                || !expected.getId().equals(actual.getId())
                || !expected.getAcceptedAt().equals(actual.getAcceptedAt())) {
            throw new DomainException(
                    QuestError.QUEST_ACCEPTANCE_NOT_FOUND
            );
        }
    }

    private QuestSignal manualCheckSignal(
            QuestCode questCode,
            Quest quest,
            QuestAcceptance acceptance,
            Long playerId,
            Instant checkedAt
    ) {
        return QuestSignal.setProgress(
                        questCode,
                        playerId,
                        quest.target().value()
                )
                .acceptancePolicy(
                        QuestSignalAcceptancePolicy.EXISTING_ONLY
                )
                .occurredAt(checkedAt)
                .periodKey(acceptance.getPeriodKey())
                .acceptanceAttempt(
                        acceptance.getId(),
                        acceptance.getAcceptedAt()
                )
                .correlationId(
                        "manual-check:acceptance:%d:accepted-at:%d".formatted(
                                acceptance.getId(),
                                acceptance.getAcceptedAt().toEpochMilli()
                        )
                )
                .attribute("acceptanceId", acceptance.getId())
                .attribute("manualCheck", true)
                .attribute("source", "USER_CONFIRMATION")
                .build();
    }
}
