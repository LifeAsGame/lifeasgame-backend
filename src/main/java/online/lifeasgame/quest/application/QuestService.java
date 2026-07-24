package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.reward.application.internal.RewardProfileLookupApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestBlueprintCatalog questBlueprintCatalog;
    private final QuestReader questReader;
    private final QuestWriter questWriter;
    private final RewardProfileLookupApi rewardProfileLookupApi;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public QuestResult.Definition ensureDefinition(QuestCommand.EnsureDefinition command) {
        Quest quest = ensureQuest(QuestCode.parse(command.code()));
        return QuestResult.Definition.from(quest);
    }

    @Transactional(readOnly = true)
    public List<QuestResult.Definition> getDefinitions() {
        return questReader.findAll().stream()
                .map(QuestResult.Definition::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestResult.Blueprint> getCatalog() {
        return questBlueprintCatalog.all().stream()
                .map(QuestResult.Blueprint::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestResult.Definition getDefinition(QuestCommand.Definition command) {
        Quest quest = questReader.getByCode(QuestCode.parse(command.questCode()));
        return QuestResult.Definition.from(quest);
    }

    @Transactional
    public QuestResult.Definition updateDefinition(QuestCommand.UpdateDefinition command) {
        Quest quest = ensureQuest(QuestCode.parse(command.questCode()));
        assertRewardContract(command, quest);
        RewardProfileRef rewardProfileRef =
                rewardProfileRefOrNull(command.rewardProfileCode(), quest);

        quest.updateDefinition(
                targetOrNull(command),
                rewardOrNull(command, quest),
                QuestRepeatRule.parseNullable(command.repeatRule()),
                command.dueAt(),
                command.definitionVersion(),
                rewardProfileRef
        );

        var events = quest.pullEvents();
        if (!events.isEmpty()) {
            domainEventPublisher.publishAll(events);
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

    @Transactional(readOnly = true)
    public List<QuestResult.Acceptance> questAcceptances(QuestCommand.Acceptances command) {
        Quest quest = questReader.getByCode(QuestCode.parse(command.questCode()));
        return questReader.findQuestAcceptances(
                        quest.getId(),
                        QuestStatus.parseNullable(command.status())
                ).stream()
                .map(acceptance -> QuestResult.Acceptance.from(acceptance, quest))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestResult.Acceptance acceptance(QuestCommand.Acceptance command) {
        QuestAcceptance acceptance = questReader.getAcceptance(command.acceptanceId());
        Quest quest = questReader.getById(acceptance.getQuestId());
        return QuestResult.Acceptance.from(acceptance, quest);
    }

    @Transactional(readOnly = true)
    public List<QuestResult.Acceptance> playerQuests(Long playerId, QuestCommand.PlayerQuests command) {
        List<QuestAcceptance> acceptances =
                questReader.findPlayerAcceptances(
                        playerId,
                        QuestStatus.parseNullable(command.status())
                );

        if (acceptances.isEmpty()) {
            return List.of();
        }

        Set<Long> questIds = acceptances.stream()
                .map(QuestAcceptance::getQuestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Quest> questMap = questReader.getByIds(questIds).stream()
                .collect(Collectors.toMap(Quest::getId, Function.identity()));

        return acceptances.stream()
                .map(acc -> QuestResult.Acceptance.from(acc, questMap.get(acc.getQuestId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestResult.PlayerQuest playerQuest(Long playerId, QuestCommand.PlayerQuest command) {
        Quest quest = questReader.getByCode(QuestCode.parse(command.questCode()));
        QuestAcceptance latest = questReader.findLatest(quest.getId(), playerId);
        return QuestResult.PlayerQuest.from(quest, latest);
    }

    @Transactional
    public Quest ensureQuest(QuestCode code) {
        return questReader.findByCode(code)
                .orElseGet(() -> materialize(questBlueprintCatalog.require(code)));
    }

    private Quest materialize(QuestBlueprint blueprint) {
        if (blueprint.usesRewardProfile()) {
            rewardProfileLookupApi.getActiveByCode(
                    blueprint.rewardProfileCodeOrNull()
            );
        }
        return questWriter.create(blueprint.instantiate());
    }

    @Transactional
    public QuestResult.Acceptance accept(Long playerId, QuestCommand.Accept command) {
        QuestCode questCode = QuestCode.parse(command.questCode());
        Quest quest = questReader.getByCode(questCode);

        questReader.assertAcceptanceIsExists(playerId, quest.getId());

        QuestAcceptance questAcceptance = questWriter.accept(
                QuestAcceptance.start(
                        quest.getId(),
                        playerId,
                        command.partyId(),
                        command.guildId(),
                        TimePeriod.daily(LocalDate.now())
                )
        );

        return QuestResult.Acceptance.from(questAcceptance, quest);
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
        Instant transitionAt = Instant.now();
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
        Instant transitionAt = Instant.now();
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
        domainEventPublisher.publish(
                QuestEvent.builder(QuestEventType.QUEST_PROGRESS)
                        .questId(quest.getId())
                        .questCode(quest.getCode())
                        .playerId(acceptance.getPlayerId())
                        .attribute("acceptanceId", acceptance.getId())
                        .attribute("progress", acceptance.getProgressValue())
                        .attribute("target", quest.target().value())
                        .attribute("status", acceptance.getStatus().name())
                        .attribute("completionPolicy", quest.getCompletionPolicy().name())
                        .occurredAt(occurredAt)
                        .correlationId(correlation(acceptance, suffix))
                        .build()
        );
    }

    private void publishGoalReached(
            QuestAcceptance acceptance,
            Quest quest,
            Instant occurredAt,
            String suffix
    ) {
        domainEventPublisher.publish(
                QuestEvent.builder(QuestEventType.QUEST_GOAL_REACHED)
                        .questId(quest.getId())
                        .questCode(quest.getCode())
                        .playerId(acceptance.getPlayerId())
                        .attribute("acceptanceId", acceptance.getId())
                        .attribute("progress", acceptance.getProgressValue())
                        .attribute("target", quest.target().value())
                        .attribute("reachedAt", acceptance.getGoalReachedAt())
                        .attribute("completionPolicy", quest.getCompletionPolicy().name())
                        .occurredAt(occurredAt)
                        .correlationId(correlation(acceptance, suffix))
                        .build()
        );
    }

    private void publishCompleted(
            QuestAcceptance acceptance,
            Quest quest,
            String suffix
    ) {
        domainEventPublisher.publish(
                QuestEvent.builder(QuestEventType.QUEST_COMPLETED)
                        .questId(quest.getId())
                        .questCode(quest.getCode())
                        .playerId(acceptance.getPlayerId())
                        .attribute("acceptanceId", acceptance.getId())
                        .attribute("progress", acceptance.getProgressValue())
                        .attribute("target", quest.target().value())
                        .attribute("goalReachedAt", acceptance.getGoalReachedAt())
                        .attribute("completedAt", acceptance.getCompletedAt())
                        .attribute("completionPolicy", quest.getCompletionPolicy().name())
                        .attribute(
                                "questDefinitionVersion",
                                quest.getDefinitionVersion()
                        )
                        .attribute(
                                "rewardProfileCode",
                                quest.rewardProfileCodeOrNull()
                        )
                        .occurredAt(acceptance.getCompletedAt())
                        .correlationId(correlation(acceptance, suffix))
                        .build()
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
