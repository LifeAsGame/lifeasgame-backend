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

    public MailboxResult.Slot deliver(MailboxCommand.Deliver command) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        return mailboxService.deliver(playerId, command);
    }

    public void claim(MailboxCommand.Claim command) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        mailboxService.claim(playerId, command);
    }

    public MailboxResult.Entries list() {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        return mailboxService.list(playerId);
    }

    public void claimAll(MailboxCommand.ClaimAll command) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        mailboxService.claimAll(playerId, command);
    }
}
