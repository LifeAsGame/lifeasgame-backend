package online.lifeasgame.inventory.application.query;

import java.util.List;

public interface MailBoxQuery {

    List<MailboxEntryView> findMailBoxEntries(Long playerId);
}
