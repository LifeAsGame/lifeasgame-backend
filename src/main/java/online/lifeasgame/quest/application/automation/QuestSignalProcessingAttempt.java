package online.lifeasgame.quest.application.automation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.PlayerTimezoneResolver;
import online.lifeasgame.quest.application.QuestDefinitionProvisioner;
import online.lifeasgame.quest.application.event.QuestCompletionEventFactory;
import online.lifeasgame.quest.application.event.QuestTransitionEventFactory;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import online.lifeasgame.quest.domain.repository.QuestSignalReceiptRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.*;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestSignalProcessingAttempt {

    private final QuestSignalReceiptRepository receiptRepository;
    private final QuestDefinitionProvisioner definitionProvisioner;
    private final QuestAcceptanceRepository questAcceptanceRepository;
    private final QuestProgressStore questProgressStore;
    private final DomainEventPublisher domainEventPublisher;
    private final QuestCompletionEventFactory completionEventFactory;
    private final QuestTransitionEventFactory transitionEventFactory;
    private final PlayerTimezoneResolver playerTimezoneResolver;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QuestSignalProcessingResult process(
            QuestSignal signal,
            String payloadFingerprint
    ) {
        QuestSignalReceipt receipt = receiptRepository.saveAndFlush(
                QuestSignalReceipt.create(
                        signal.questCode().value(),
                        signal.playerId(),
                        signal.correlationId(),
                        signal.type().name(),
                        payloadFingerprint,
                        signal.occurredAt()
                )
        );

        Quest quest = definitionProvisioner.ensure(signal.questCode());
        ZoneId playerZone = Objects.requireNonNull(
                playerTimezoneResolver.resolve(signal.playerId()),
                "playerTimezone"
        );
        LocalDate eventDate = LocalDateTime.ofInstant(
                signal.occurredAt(),
                playerZone
        ).toLocalDate();
        Optional<QuestAcceptance> acceptanceOpt =
                resolveAcceptance(signal, quest, eventDate);
        if (acceptanceOpt.isEmpty()) {
            return QuestSignalProcessingResult.applied(receipt.getId());
        }

        QuestAcceptance acceptance = acceptanceOpt.get();
        boolean newlyCreated = acceptance.getId() == null;
        if (newlyCreated) {
            acceptance = questAcceptanceRepository.save(acceptance);
            publishAccepted(signal, quest, acceptance);
        }

        if (!acceptance.isInProgress()) {
            return QuestSignalProcessingResult.applied(receipt.getId());
        }
        if (!accepts(signal, acceptance)) {
            return QuestSignalProcessingResult.applied(receipt.getId());
        }

        int progressValue = progressValue(signal, acceptance);
        acceptance.setProgress(progressValue, quest, signal.occurredAt());
        boolean goalReached = acceptance.isGoalReached();
        acceptance = questAcceptanceRepository.save(acceptance);

        publishProgress(signal, quest, acceptance);
        if (goalReached) {
            publishGoalReached(signal, quest, acceptance);
            if (quest.isAutoCompletion()
                    && acceptance.complete(signal.occurredAt())) {
                acceptance = questAcceptanceRepository.save(acceptance);
                publishCompleted(signal, quest, acceptance);
            }
        }

        synchronizeProgressStoreAfterCommit(
                signal,
                quest,
                eventDate,
                playerZone,
                progressValue,
                goalReached
        );
        return QuestSignalProcessingResult.applied(receipt.getId());
    }

    private int progressValue(
            QuestSignal signal,
            QuestAcceptance acceptance
    ) {
        if (signal.isSetOperation()) {
            return signal.progressValue();
        }
        return Math.addExact(
                acceptance.getProgressValue(),
                signal.progressDelta()
        );
    }

    private Optional<QuestAcceptance> resolveAcceptance(
            QuestSignal signal,
            Quest quest,
            LocalDate eventDate
    ) {
        Optional<QuestAcceptance> latest =
                questAcceptanceRepository.findLatestByQuestAndPlayer(
                        quest.getId(),
                        signal.playerId()
                );
        if (latest.isPresent()) {
            QuestAcceptance current = latest.get();
            if (current.isInProgress()
                    && current.getPeriod().contains(eventDate)) {
                return Optional.of(current);
            }
            if (current.isGoalReached()) {
                return Optional.empty();
            }
            if (current.isCompleted()) {
                if (quest.getRepeatRule().isOneTime()
                        || current.getPeriod().contains(eventDate)) {
                    return Optional.empty();
                }
            }
            if (current.isCanceled()
                    && (quest.getRepeatRule().isOneTime()
                    || current.getPeriod().contains(eventDate))) {
                return Optional.empty();
            }
        }

        if (signal.acceptancePolicy()
                == QuestSignalAcceptancePolicy.EXISTING_ONLY) {
            return Optional.empty();
        }

        TimePeriod period = periodFor(quest, eventDate);
        QuestAcceptance acceptance = QuestAcceptance.start(
                quest.getId(),
                signal.playerId(),
                period,
                signal.occurredAt(),
                signal.periodKey()
        );
        acceptance.assignIdempotencyKey(signal.correlationId());
        return Optional.of(acceptance);
    }

    private TimePeriod periodFor(Quest quest, LocalDate eventDate) {
        return quest.getRepeatRule().periodFor(eventDate);
    }

    private Duration ttlFor(
            Quest quest,
            LocalDate eventDate,
            ZoneId playerZone
    ) {
        if (quest.getRepeatRule().isOneTime()) {
            return null;
        }
        TimePeriod period = periodFor(quest, eventDate);
        LocalDateTime now = LocalDateTime.ofInstant(
                clock.instant(),
                playerZone
        );
        LocalDateTime endExclusive = period.end().plusDays(1).atStartOfDay();
        if (endExclusive.isBefore(now)) {
            return Duration.ZERO;
        }
        return Duration.between(now, endExclusive);
    }

    private void synchronizeProgressStoreAfterCommit(
            QuestSignal signal,
            Quest quest,
            LocalDate eventDate,
            ZoneId playerZone,
            int progressValue,
            boolean goalReached
    ) {
        Runnable synchronization = () -> {
            try {
                if (goalReached) {
                    questProgressStore.reset(
                            signal.questCode(),
                            signal.playerId()
                    );
                } else {
                    questProgressStore.set(
                            signal.questCode(),
                            signal.playerId(),
                            progressValue,
                            ttlFor(quest, eventDate, playerZone)
                    );
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "Failed to synchronize QuestProgressStore for quest {} and player {}",
                        signal.questCode().value(),
                        signal.playerId(),
                        exception
                );
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            synchronization.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        synchronization.run();
                    }
                }
        );
    }

    private boolean accepts(
            QuestSignal signal,
            QuestAcceptance acceptance
    ) {
        if (signal.acceptancePolicy()
                != QuestSignalAcceptancePolicy.EXISTING_ONLY) {
            return true;
        }
        if (signal.occurredAt().isBefore(acceptance.getAcceptedAt())) {
            return false;
        }
        if (!Objects.equals(
                signal.periodKey(),
                acceptance.getPeriodKey()
        )) {
            return false;
        }
        return !signal.hasAcceptanceAttemptContext()
                || signal.matchesAcceptanceAttempt(
                acceptance.getId(),
                acceptance.getAcceptedAt()
        );
    }

    private void publishAccepted(
            QuestSignal signal,
            Quest quest,
            QuestAcceptance acceptance
    ) {
        domainEventPublisher.publish(transitionEventFactory.accepted(
                acceptance,
                quest,
                signal.attributes(),
                signal.occurredAt(),
                correlation(signal, "accepted")
        ));
    }

    private void publishProgress(
            QuestSignal signal,
            Quest quest,
            QuestAcceptance acceptance
    ) {
        domainEventPublisher.publish(transitionEventFactory.progress(
                acceptance,
                quest,
                signal.attributes(),
                signal.type() == QuestSignalType.ADD_PROGRESS
                        ? signal.progressDelta()
                        : null,
                signal.occurredAt(),
                correlation(signal, "progress")
        ));
    }

    private void publishGoalReached(
            QuestSignal signal,
            Quest quest,
            QuestAcceptance acceptance
    ) {
        domainEventPublisher.publish(transitionEventFactory.goalReached(
                acceptance,
                quest,
                signal.attributes(),
                signal.occurredAt(),
                correlation(signal, "goal-reached")
        ));
    }

    private void publishCompleted(
            QuestSignal signal,
            Quest quest,
            QuestAcceptance acceptance
    ) {
        domainEventPublisher.publish(
                completionEventFactory.create(
                        acceptance,
                        quest,
                        correlation(signal, "completed"),
                        signal.attributes()
                )
        );
    }

    private String correlation(QuestSignal signal, String suffix) {
        return signal.correlationId() + ":" + suffix;
    }
}
