package online.lifeasgame.inventory.application.query;

import java.util.List;

public interface MailboxQuery {

    List<MailboxEntryView> findMailboxEntries(Long playerId);
}
