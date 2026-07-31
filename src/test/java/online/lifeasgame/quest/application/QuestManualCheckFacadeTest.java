package online.lifeasgame.quest.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("QuestFacade manual-check")
class QuestManualCheckFacadeTest {

    @Test
    @DisplayName("인증된 CurrentPlayer로 Manual Check를 수행한다")
    void usesCurrentPlayer() {
        QuestService questService = mock(QuestService.class);
        QuestManualCheckService manualCheckService =
                mock(QuestManualCheckService.class);
        CurrentPlayerAccessor currentPlayerAccessor =
                mock(CurrentPlayerAccessor.class);
        QuestCommand.ManualCheck command =
                new QuestCommand.ManualCheck("Q_GROWTH_ONE_FOCUS");
        QuestResult.Acceptance expected =
                mock(QuestResult.Acceptance.class);
        when(currentPlayerAccessor.currentPlayerIdOrThrow())
                .thenReturn(217001L);
        when(manualCheckService.check(217001L, command))
                .thenReturn(expected);
        QuestFacade facade = new QuestFacade(
                questService,
                manualCheckService,
                currentPlayerAccessor
        );

        assertThat(facade.manualCheck(command)).isSameAs(expected);
        verify(currentPlayerAccessor).currentPlayerIdOrThrow();
        verify(manualCheckService).check(217001L, command);
    }
}
