package online.lifeasgame.quest.application.automation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestRepeatRule;
import online.lifeasgame.quest.domain.TimePeriod;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuestAutomationService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private final QuestService questService;
    private final QuestAcceptanceRepository questAcceptanceRepository;
    private final QuestProgressStore questProgressStore;
    private final DomainEventPublisher domainEventPublisher;
    private final QuestSignalIdempotencyGuard questSignalIdempotencyGuard;

    public void processSignals(Collection<QuestSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return;
        }
        signals.forEach(this::processSignal);
    }

    private void processSignal(QuestSignal signal) {
        Quest quest = questService.ensureQuest(signal.questCode());

        if (!questSignalIdempotencyGuard.accept(quest, signal)) {
            log.trace(
                    "Skipping quest signal {} for player {} due to duplicate correlation {}",
                    signal.questCode().value(),
                    signal.playerId(),
                    signal.correlationId()
            );
            return;
        }

        LocalDate eventDate = LocalDateTime.ofInstant(signal.occurredAt(), DEFAULT_ZONE).toLocalDate();

        Optional<QuestAcceptance> acceptanceOpt = resolveAcceptance(signal, quest, eventDate);
        if (acceptanceOpt.isEmpty()) {
            return;
        }

        QuestAcceptance acceptance = acceptanceOpt.get();
        boolean newlyCreated = acceptance.getId() == null;

        if (newlyCreated) {
            questProgressStore.reset(signal.questCode(), signal.playerId());
            acceptance = questAcceptanceRepository.save(acceptance);
            publishAccepted(signal, quest, acceptance);
        }

        if (!acceptance.isInProgress()) {
            return;
        }

        Duration ttl = ttlFor(quest, signal.occurredAt(), eventDate);
        if (!newlyCreated && acceptance.getProgressValue() > 0 && signal.type() == QuestSignalType.ADD_PROGRESS) {
            questProgressStore.set(signal.questCode(), signal.playerId(), acceptance.getProgressValue(), ttl);
        }

        int progressValue = signal.isSetOperation()
                ? questProgressStore.set(signal.questCode(), signal.playerId(), signal.progressValue(), ttl)
                : questProgressStore.increment(signal.questCode(), signal.playerId(), signal.progressDelta(), ttl);

        acceptance.setProgress(progressValue, quest);
        acceptance = questAcceptanceRepository.save(acceptance);

        publishProgress(signal, quest, acceptance, progressValue);

        if (acceptance.isDone()) {
            questProgressStore.reset(signal.questCode(), signal.playerId());
            publishCompleted(signal, quest, acceptance, progressValue);
        }
    }

    private Optional<QuestAcceptance> resolveAcceptance(QuestSignal signal, Quest quest, LocalDate eventDate) {
        Optional<QuestAcceptance> latest = questAcceptanceRepository.findLatestByQuestAndPlayer(quest.getId(), signal.playerId());
        if (latest.isPresent()) {
            QuestAcceptance current = latest.get();
            if (current.isInProgress() && current.getPeriod().contains(eventDate)) {
                return Optional.of(current);
            }
            if (current.isDone()) {
                if (quest.getRepeatRule() == QuestRepeatRule.NONE) {
                    return Optional.empty();
                }
                if (current.getPeriod().contains(eventDate)) {
                    return Optional.empty();
                }
            }
        }

        TimePeriod period = periodFor(quest, eventDate);
        QuestAcceptance acceptance = QuestAcceptance.start(quest.getId(), signal.playerId(), period);
        acceptance.assignIdempotencyKey(signal.correlationId());
        return Optional.of(acceptance);
    }

    private TimePeriod periodFor(Quest quest, LocalDate eventDate) {
        return switch (quest.getRepeatRule()) {
            case NONE -> TimePeriod.forever();
            case DAILY -> TimePeriod.daily(eventDate);
            case WEEKLY -> TimePeriod.weekly(eventDate);
            case MONTHLY -> TimePeriod.monthly(eventDate);
        };
    }

    private Duration ttlFor(Quest quest, Instant occurredAt, LocalDate eventDate) {
        if (quest.getRepeatRule() == QuestRepeatRule.NONE) {
            return null;
        }
        TimePeriod period = periodFor(quest, eventDate);
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        LocalDateTime endExclusive = period.end().plusDays(1).atStartOfDay();
        if (endExclusive.isBefore(now)) {
            return Duration.ZERO;
        }
        return Duration.between(now, endExclusive);
    }

    private void publishAccepted(QuestSignal signal, Quest quest, QuestAcceptance acceptance) {
        domainEventPublisher.publish(
                QuestEvent.builder(QuestEventType.QUEST_ACCEPTED)
                        .questId(quest.getId())
                        .questCode(quest.getCode())
                        .playerId(signal.playerId())
                        .attribute("progress", acceptance.getProgressValue())
                        .attribute("target", quest.target().value())
                        .attribute("repeatRule", quest.getRepeatRule().name())
                        .attributes(signal.attributes())
                        .occurredAt(signal.occurredAt())
                        .correlationId(correlation(signal, "accepted"))
                        .build()
        );
    }

    private void publishProgress(QuestSignal signal, Quest quest, QuestAcceptance acceptance, int progressValue) {
        domainEventPublisher.publish(
                QuestEvent.builder(QuestEventType.QUEST_PROGRESS)
                        .questId(quest.getId())
                        .questCode(quest.getCode())
                        .playerId(signal.playerId())
                        .attribute("progress", progressValue)
                        .attribute("target", quest.target().value())
                        .attribute("repeatRule", quest.getRepeatRule().name())
                        .attribute("status", acceptance.getStatus().name())
                        .attribute("delta", signal.type() == QuestSignalType.ADD_PROGRESS ? signal.progressDelta() : null)
                        .attributes(signal.attributes())
                        .occurredAt(signal.occurredAt())
                        .correlationId(correlation(signal, "progress"))
                        .build()
        );
    }

    private void publishCompleted(QuestSignal signal, Quest quest, QuestAcceptance acceptance, int progressValue) {
        domainEventPublisher.publish(
                QuestEvent.builder(QuestEventType.QUEST_COMPLETED)
                        .questId(quest.getId())
                        .questCode(quest.getCode())
                        .playerId(signal.playerId())
                        .attribute("progress", progressValue)
                        .attribute("target", quest.target().value())
                        .attribute("repeatRule", quest.getRepeatRule().name())
                        .attribute("completedAt", acceptance.getCompletedAt())
                        .attributes(signal.attributes())
                        .occurredAt(signal.occurredAt())
                        .correlationId(correlation(signal, "completed"))
                        .build()
        );
    }

    private String correlation(QuestSignal signal, String suffix) {
        if (signal.correlationId() != null && !signal.correlationId().isBlank()) {
            return signal.correlationId() + ":" + suffix;
        }
        return "%s:%s:%s".formatted(signal.questCode().value(), signal.playerId(), suffix);
    }
}
