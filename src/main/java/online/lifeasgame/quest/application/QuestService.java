package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.event.QuestCompletionEventFactory;
import online.lifeasgame.quest.application.event.QuestDefinitionEventFactory;
import online.lifeasgame.quest.application.event.QuestTransitionEventFactory;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestDefinitionProvisioner definitionProvisioner;
    private final QuestReader questReader;
    private final QuestWriter questWriter;
    private final RewardProfileLookupApi rewardProfileLookupApi;
    private final DomainEventPublisher domainEventPublisher;
    private final QuestCompletionEventFactory completionEventFactory;
    private final QuestDefinitionEventFactory definitionEventFactory;
    private final QuestTransitionEventFactory transitionEventFactory;
    private final PlayerTimezoneResolver playerTimezoneResolver;
    private final Clock clock;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public QuestResult.Definition ensureDefinition(QuestCommand.EnsureDefinition command) {
        Quest quest = definitionProvisioner.ensure(
                QuestCode.parse(command.code())
        );
        return QuestResult.Definition.from(quest);
    }

    @Transactional
    public QuestResult.Definition updateDefinition(QuestCommand.UpdateDefinition command) {
        Quest quest = definitionProvisioner.ensure(
                QuestCode.parse(command.questCode())
        );
        assertRewardContract(command, quest);
        RewardProfileRef rewardProfileRef =
                rewardProfileRefOrNull(command.rewardProfileCode(), quest);

        boolean changed = quest.updateDefinition(
                targetOrNull(command),
                rewardOrNull(command, quest),
                QuestRepeatRule.parseNullable(command.repeatRule()),
                command.dueAt(),
                command.definitionVersion(),
                rewardProfileRef,
                QuestSemanticCategory.parseNullable(
                        command.semanticCategory()
                ),
                QuestProgressSource.parseNullable(command.progressSource()),
                QuestRepeatRule.parsePolicyNullable(command.repeatPolicy()),
                roleTemplateRefOrNull(command.roleTemplateCode())
        );

        if (changed) {
            domainEventPublisher.publish(definitionEventFactory.updated(
                    quest,
                    clock.instant()
            ));
        }

        return QuestResult.Definition.from(quest);
    }

    private QuestTarget targetOrNull(QuestCommand.UpdateDefinition command) {
        if (command.targetType() == null && command.targetValue() == null) return null;
        if (command.targetType() == null || command.targetValue() == null) {
            throw new IllegalArgumentException("targetType and targetValue must be provided together.");
        }
        return QuestTarget.of(QuestTargetType.parse(command.targetType()), command.targetValue());
    }

    private QuestReward rewardOrNull(QuestCommand.UpdateDefinition c, Quest quest) {
        if (c.rewardExp() == null && c.rewardStats() == null) return null;

        int exp = (c.rewardExp() != null) ? c.rewardExp() : quest.getReward().exp();
        RewardStats stats = (c.rewardStats() != null) ? new RewardStats(c.rewardStats()) : quest.getReward().stats();
        return QuestReward.of(exp, stats);
    }

    private void assertRewardContract(
            QuestCommand.UpdateDefinition command,
            Quest quest
    ) {
        boolean changesInlineReward =
                command.rewardExp() != null || command.rewardStats() != null;
        if (changesInlineReward
                && (command.rewardProfileCode() != null
                || quest.usesRewardProfile())) {
            throw new DomainException(QuestError.QUEST_REWARD_CONTRACT_CONFLICT);
        }
    }

    private RewardProfileRef rewardProfileRefOrNull(
            String rewardProfileCode,
            Quest quest
    ) {
        if (rewardProfileCode == null) {
            return null;
        }
        RewardProfileRef requested = RewardProfileRef.of(rewardProfileCode);
        if (requested.code().equals(quest.rewardProfileCodeOrNull())) {
            return null;
        }
        RewardProfileLookupApi.RewardProfileReference reference =
                rewardProfileLookupApi.getActiveByCode(requested.code());
        return RewardProfileRef.of(reference.code());
    }

    private QuestRoleTemplateRef roleTemplateRefOrNull(String code) {
        return code == null ? null : QuestRoleTemplateRef.of(code);
    }

    @Transactional
    public QuestResult.Acceptance accept(QuestCommand.Accept command) {
        return accept(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public QuestResult.Acceptance accept(Long playerId, QuestCommand.Accept command) {
        QuestCode questCode = QuestCode.parse(command.questCode());
        Quest quest = questReader.getByCode(questCode);
        Instant acceptedAt = clock.instant();
        ZoneId playerZone = Objects.requireNonNull(
                playerTimezoneResolver.resolve(playerId),
                "playerTimezone"
        );
        LocalDate acceptedDate = acceptedAt.atZone(playerZone).toLocalDate();
        TimePeriod period = quest.getRepeatRule().periodFor(acceptedDate);
        String periodKey = questCode == QuestCode.Q_RECORD_WEEKLY_LOOKBACK
                ? weeklyPeriodKey(acceptedAt, playerZone)
                : null;
        QuestAcceptance latest = questReader.findLatest(
                quest.getId(),
                playerId
        );
        if (latest != null && latest.getPeriod().contains(acceptedDate)) {
            if (latest.isCanceled()) {
                latest.restart(
                        command.partyId(),
                        command.guildId(),
                        acceptedAt,
                        periodKey
                );
                QuestAcceptance restarted =
                        questWriter.saveAcceptance(latest);
                return QuestResult.Acceptance.from(restarted, quest);
            }
            throw new DomainException(
                    QuestError.QUEST_ACCEPTANCE_ALREADY_EXISTS
            );
        }

        QuestAcceptance questAcceptance = questWriter.saveAcceptance(
                QuestAcceptance.start(
                        quest.getId(),
                        playerId,
                        command.partyId(),
                        command.guildId(),
                        period,
                        acceptedAt,
                        periodKey
                )
        );

        return QuestResult.Acceptance.from(questAcceptance, quest);
    }

    private String weeklyPeriodKey(Instant acceptedAt, ZoneId playerZone) {
        ZonedDateTime local = acceptedAt.atZone(playerZone);
        WeekFields iso = WeekFields.ISO;
        return "%04d-W%02d".formatted(
                local.get(iso.weekBasedYear()),
                local.get(iso.weekOfWeekBasedYear())
        );
    }

    @Transactional
    public QuestResult.Canceled cancel(QuestCommand.Cancel command) {
        return cancel(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public QuestResult.Canceled cancel(Long playerId, QuestCommand.Cancel command) {
        QuestCode questCode = QuestCode.parse(command.questCode());
        Quest quest = questReader.getByCode(questCode);
        QuestAcceptance acceptance = questReader.findLatest(quest.getId(), playerId);
        if (acceptance == null) {
            throw new DomainException(QuestError.QUEST_ACCEPTANCE_NOT_FOUND);
        }
        acceptance.cancel();
        questWriter.saveAcceptance(acceptance);

        return new QuestResult.Canceled(playerId, quest.getId(), questCode.name());
    }

    @Transactional
    public QuestResult.Acceptance adjustAcceptanceProgress(Long acceptanceId, QuestCommand.AdjustProgress command) {
        QuestAcceptance acceptance = questReader.getAcceptance(acceptanceId);
        Quest quest = questReader.getById(acceptance.getQuestId());
        Instant transitionAt = clock.instant();
        acceptance.addProgress(command.delta(), quest, transitionAt);
        publishProgress(acceptance, quest, transitionAt, "admin-progress");
        if (acceptance.isGoalReached()) {
            publishGoalReached(acceptance, quest, transitionAt, "admin-goal-reached");
            if (quest.isAutoCompletion() && acceptance.complete(transitionAt)) {
                publishCompleted(acceptance, quest, "admin-completed");
            }
        }

        return QuestResult.Acceptance.from(acceptance, quest);
    }

    @Transactional
    public QuestResult.Acceptance changeAcceptanceStatus(Long acceptanceId, QuestCommand.ChangeStatus command) {
        QuestStatus questStatus = QuestStatus.parse(command.status());
        QuestAcceptance acceptance = questReader.getAcceptance(acceptanceId);
        Quest quest = questReader.getById(acceptance.getQuestId());
        Instant transitionAt = clock.instant();
        boolean changed = acceptance.changeStatus(questStatus, transitionAt);
        if (changed && acceptance.isGoalReached()) {
            publishGoalReached(acceptance, quest, transitionAt, "admin-goal-reached");
        }
        if (changed && acceptance.isCompleted()) {
            publishCompleted(acceptance, quest, "admin-completed");
        }
        return QuestResult.Acceptance.from(acceptance, quest);
    }

    private void publishProgress(
            QuestAcceptance acceptance,
            Quest quest,
            Instant occurredAt,
            String suffix
    ) {
        domainEventPublisher.publish(transitionEventFactory.progress(
                acceptance,
                quest,
                Map.of(),
                null,
                occurredAt,
                correlation(acceptance, suffix)
        ));
    }

    private void publishGoalReached(
            QuestAcceptance acceptance,
            Quest quest,
            Instant occurredAt,
            String suffix
    ) {
        domainEventPublisher.publish(transitionEventFactory.goalReached(
                acceptance,
                quest,
                Map.of(),
                occurredAt,
                correlation(acceptance, suffix)
        ));
    }

    private void publishCompleted(
            QuestAcceptance acceptance,
            Quest quest,
            String suffix
    ) {
        domainEventPublisher.publish(
                completionEventFactory.create(
                        acceptance,
                        quest,
                        correlation(acceptance, suffix)
                )
        );
    }

    private String correlation(QuestAcceptance acceptance, String suffix) {
        return "quest:%d:acceptance:%d:%s".formatted(
                acceptance.getQuestId(),
                acceptance.getId(),
                suffix
        );
    }
}
