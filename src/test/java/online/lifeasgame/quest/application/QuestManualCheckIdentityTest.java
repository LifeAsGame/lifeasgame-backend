package online.lifeasgame.quest.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.automation.QuestSignalProcessingService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("QuestManualCheckService identity")
class QuestManualCheckIdentityTest {

    @Test
    @DisplayName("self use case는 Application Service에서 CurrentPlayer를 해석한다")
    void usesCurrentPlayerInsideApplicationService() {
        CurrentPlayerAccessor currentPlayerAccessor =
                mock(CurrentPlayerAccessor.class);
        QuestManualCheckService service = spy(new QuestManualCheckService(
                mock(QuestReader.class),
                mock(QuestSignalProcessingService.class),
                mock(QuestAcceptanceCompletionService.class),
                mock(PlayerTimezoneResolver.class),
                mock(Clock.class),
                currentPlayerAccessor
        ));
        QuestCommand.ManualCheck command =
                new QuestCommand.ManualCheck("Q_GROWTH_ONE_FOCUS");
        QuestResult.Acceptance expected =
                mock(QuestResult.Acceptance.class);
        when(currentPlayerAccessor.currentPlayerIdOrThrow())
                .thenReturn(217001L);
        doReturn(expected).when(service).check(217001L, command);

        assertThat(service.check(command)).isSameAs(expected);

        verify(currentPlayerAccessor).currentPlayerIdOrThrow();
        verify(service).check(217001L, command);
    }
}
