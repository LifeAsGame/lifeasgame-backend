package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        quest.updateDefinition(
                targetOrNull(command),
                rewardOrNull(command, quest),
                QuestRepeatRule.parseNullable(command.repeatRule()),
                command.dueAt()
        );

        domainEventPublisher.publishAll(quest.pullEvents());

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

    @Transactional(readOnly = true)
    public List<QuestResult.Acceptance> questAcceptances(QuestCommand.Acceptances command) {
        Quest quest = questReader.getByCode(QuestCode.parse(command.questCode()));
        return questReader.findQuestAcceptances(quest.getId(), QuestStatus.parse(command.status())).stream()
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
                questReader.findPlayerAcceptances(playerId, QuestStatus.parse(command.status()));

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
                .orElseGet(() -> questWriter.create(questBlueprintCatalog.require(code).instantiate()));
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

        questWriter.cancel(playerId, quest.getId());

        return new QuestResult.Canceled(playerId, quest.getId(), questCode.name());
    }

    @Transactional
    public QuestResult.Acceptance adjustAcceptanceProgress(Long acceptanceId, QuestCommand.AdjustProgress command) {
        QuestAcceptance acceptance = questReader.getAcceptance(acceptanceId);
        Quest quest = questReader.getById(acceptance.getQuestId());
        acceptance.addProgress(command.delta(), quest);

        return QuestResult.Acceptance.from(acceptance, quest);
    }

    @Transactional
    public QuestResult.Acceptance changeAcceptanceStatus(Long acceptanceId, QuestCommand.ChangeStatus command) {
        QuestStatus questStatus = QuestStatus.parse(command.status());
        QuestAcceptance acceptance = questReader.getAcceptance(acceptanceId);
        acceptance.changeStatus(questStatus);
        Quest quest = questReader.getById(acceptance.getQuestId());
        return QuestResult.Acceptance.from(acceptance, quest);
    }
}
