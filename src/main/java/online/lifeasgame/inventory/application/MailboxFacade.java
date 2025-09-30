package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.MailboxResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailboxFacade {

    private final MailboxService mailboxService;
    private final CurrentPlayerAccessor currentPlayer;

    public MailboxResult.Slot deliver(MailboxCommand.Deliver cmd) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        return mailboxService.deliver(playerId, cmd);
    }

    public void claim(MailboxCommand.Claim cmd) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        mailboxService.claim(playerId, cmd);
    }

    public MailboxResult.Mails list() {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        return mailboxService.list(playerId);
    }
}
