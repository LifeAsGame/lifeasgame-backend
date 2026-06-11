package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.query.MailboxEntryView;
import online.lifeasgame.inventory.application.query.MailBoxQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class MailBoxQueryReader {

    private final MailBoxQuery mailBoxQuery;

    public List<MailboxEntryView> list(Long playerId) {
        return mailBoxQuery.findMailBoxEntries(playerId);
    }
}
