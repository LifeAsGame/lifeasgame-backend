package online.lifeasgame.inventory.infra;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.query.MailboxEntryView;
import online.lifeasgame.inventory.application.query.MailboxQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MailboxQueryAdapter implements MailboxQuery {

    private final EntityManager em;

    @Override
    public List<MailboxEntryView> findMailboxEntries(Long playerId) {
        return em.createQuery("""
            SELECT new online.lifeasgame.inventory.application.query.MailboxEntryView(
                m.id,
                m.slotIndex.value,
                i.id,
                i.name.value,
                i.category,
                i.type,
                i.rarity,
                i.stackable,
                i.maxStack,
                m.quantity.value,
                m.bound,
                m.durability.value,
                m.instAttrs
            )
            FROM MailboxEntry m
            JOIN m.mailbox mbx
            JOIN Item i ON i.id = m.itemId
            WHERE mbx.playerId = :playerId
            ORDER BY m.slotIndex.value
        """, MailboxEntryView.class)
                .setParameter("playerId", playerId)
                .getResultList();
    }
}
