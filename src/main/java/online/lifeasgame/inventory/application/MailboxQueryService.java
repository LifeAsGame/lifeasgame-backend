package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.query.MailboxEntryView;
import online.lifeasgame.inventory.application.query.MailboxQuery;
import online.lifeasgame.inventory.application.result.MailboxResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MailboxQueryService {

    private final MailboxQuery mailboxQuery;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public MailboxResult.Entries list() {
        return list(currentPlayerAccessor.currentPlayerIdOrThrow());
    }

    public MailboxResult.Entries list(Long playerId) {
        List<MailboxEntryView> entryViews =
                mailboxQuery.findMailboxEntries(playerId);
        return MailboxResult.Entries.fromViews(entryViews);
    }
}
