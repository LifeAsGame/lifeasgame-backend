package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestFacade {

    private final QuestService questService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public List<QuestResult.Acceptance> list(QuestCommand.PlayerQuests command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return questService.playerQuests(playerId, command);
    }

    public QuestResult.PlayerQuest detail(QuestCommand.PlayerQuest command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return questService.playerQuest(playerId, command);
    }

    public QuestResult.Acceptance accept(QuestCommand.Accept command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return questService.accept(playerId, command);
    }
}
