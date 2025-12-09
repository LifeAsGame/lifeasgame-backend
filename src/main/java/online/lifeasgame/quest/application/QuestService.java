package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.repository.QuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestRepository questRepository;
    private final QuestBlueprintCatalog questBlueprintCatalog;
    private final QuestReader questReader;
    private final QuestWriter questWriter;

    @Transactional
    public QuestResult.Definition ensureDefinition(QuestCommand.EnsureDefinition command) {
        return QuestResult.Definition.from(ensureQuest(QuestCode.fromValue(command.code())));
    }

    @Transactional(readOnly = true)
    public List<QuestResult.Definition> definitions() {
        return questReader.findAll().stream()
                .map(QuestResult.Definition::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestResult.Blueprint> catalog() {
        return questBlueprintCatalog.all().stream()
                .map(QuestResult.Blueprint::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestResult.Definition definition(QuestCommand.Definition command) {
        return QuestResult.Definition.from(questReader.getByCode(QuestCode.fromValue(command.questCode())));
    }

    @Transactional
    public QuestResult.Definition updateDefinition(QuestCommand.UpdateDefinition command) {
        Quest quest = ensureQuest(QuestCode.fromValue(command.questCode()));

        if (command.targetType() != null || command.targetValue() != null) {
            Guard.notNull(command.targetType(), "targetType");
            Guard.notNull(command.targetValue(), "targetValue");
            quest.changeTarget(QuestTarget.of(QuestTargetType.valueOf(command.targetType().toUpperCase(Locale.ROOT)), command.targetValue()));
        }

        if (command.rewardExp() != null || command.rewardStats() != null) {
            int exp = command.rewardExp() == null ? quest.getReward().exp() : command.rewardExp();
            RewardStats stats = command.rewardStats() == null ? quest.getReward().stats() : new RewardStats(command.rewardStats());
            quest.changeReward(QuestReward.of(exp, stats));
        }

        if (command.repeatRule() != null) {
            quest.changeRepeatRule(QuestRepeatRule.valueOf(command.repeatRule().toUpperCase(Locale.ROOT)));
        }

        if (command.dueAt() != null) {
            quest.reschedule(command.dueAt());
        }

        questWriter.update(quest);
        return QuestResult.Definition.from(quest);
    }

    @Transactional(readOnly = true)
    public List<QuestResult.Acceptance> questAcceptances(QuestCommand.Acceptances command) {
        Quest quest = questReader.getByCode(QuestCode.fromValue(command.questCode()));
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
        List<QuestAcceptance> acceptances = questReader.findPlayerAcceptances(playerId, QuestStatus.parse(command.status()));

        return acceptances.stream()
                .map(acceptance -> QuestResult.Acceptance.from(acceptance, questReader.getById(acceptance.getQuestId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestResult.PlayerQuest playerQuest(Long playerId, QuestCommand.PlayerQuest command) {
        Quest quest = questReader.getByCode(QuestCode.fromValue(command.questCode()));
        QuestAcceptance latest = questReader.findLatest(quest.getId(), playerId);
        return QuestResult.PlayerQuest.from(quest, latest);
    }

    @Transactional(readOnly = true)
    public Quest ensureQuest(QuestCode code) {
        return questRepository.findByCode(code.value())
                .orElseGet(() -> questWriter.create(questBlueprintCatalog.require(code).instantiate()));
    }
}
