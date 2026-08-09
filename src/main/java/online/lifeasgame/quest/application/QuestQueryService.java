package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.query.QuestQuery;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestBlueprintCatalog;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestQueryService {

    private final QuestBlueprintCatalog questBlueprintCatalog;
    private final QuestReader questReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public List<QuestResult.Definition> getDefinitions() {
        return questReader.findAll().stream()
                .map(QuestResult.Definition::from)
                .toList();
    }

    public List<QuestResult.Blueprint> getCatalog() {
        return questBlueprintCatalog.all().stream()
                .map(QuestResult.Blueprint::from)
                .toList();
    }

    public QuestResult.Definition getDefinition(QuestQuery.Definition query) {
        Quest quest = questReader.getByCode(
                QuestCode.parse(query.questCode())
        );
        return QuestResult.Definition.from(quest);
    }

    public List<QuestResult.Acceptance> questAcceptances(
            QuestQuery.Acceptances query
    ) {
        Quest quest = questReader.getByCode(
                QuestCode.parse(query.questCode())
        );
        return questReader.findQuestAcceptances(
                        quest.getId(),
                        QuestStatus.parseNullable(query.status())
                ).stream()
                .map(acceptance -> QuestResult.Acceptance.from(
                        acceptance,
                        quest
                ))
                .toList();
    }

    public QuestResult.Acceptance acceptance(QuestQuery.Acceptance query) {
        QuestAcceptance acceptance = questReader.getAcceptance(
                query.acceptanceId()
        );
        Quest quest = questReader.getById(acceptance.getQuestId());
        return QuestResult.Acceptance.from(acceptance, quest);
    }

    public List<QuestResult.Acceptance> playerQuests(
            QuestQuery.PlayerQuests query
    ) {
        return playerQuests(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                query
        );
    }

    public List<QuestResult.Acceptance> playerQuests(
            Long playerId,
            QuestQuery.PlayerQuests query
    ) {
        List<QuestAcceptance> acceptances = questReader.findPlayerAcceptances(
                playerId,
                QuestStatus.parseNullable(query.status())
        );
        if (acceptances.isEmpty()) {
            return List.of();
        }

        Set<Long> questIds = acceptances.stream()
                .map(QuestAcceptance::getQuestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Quest> quests = questReader.getByIds(questIds).stream()
                .collect(Collectors.toMap(
                        Quest::getId,
                        Function.identity()
                ));
        return acceptances.stream()
                .map(acceptance -> QuestResult.Acceptance.from(
                        acceptance,
                        quests.get(acceptance.getQuestId())
                ))
                .toList();
    }

    public QuestResult.PlayerQuest playerQuest(
            QuestQuery.PlayerQuest query
    ) {
        return playerQuest(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                query
        );
    }

    public QuestResult.PlayerQuest playerQuest(
            Long playerId,
            QuestQuery.PlayerQuest query
    ) {
        Quest quest = questReader.getByCode(
                QuestCode.parse(query.questCode())
        );
        QuestAcceptance latest = questReader.findLatest(
                quest.getId(),
                playerId
        );
        return QuestResult.PlayerQuest.from(quest, latest);
    }
}
