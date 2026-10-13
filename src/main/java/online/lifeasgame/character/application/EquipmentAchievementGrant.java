package online.lifeasgame.character.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentGrantApi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EquipmentAchievementGrant {
    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");
    private final PlayerReader players;
    private final JdbcTemplate jdbc;
    private final InventoryEquipmentGrantApi delivery;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean grantIfEligible(Long playerId, String achievementCode) {
        Equipment entitlement = Equipment.forAchievement(achievementCode);
        if (entitlement == null) return false;
        // Shared Player lock serializes event awards, reconciliation, and manual awards.
        players.getByIdForUpdateOrThrow(playerId);
        Integer owned = jdbc.queryForObject("""
                SELECT COUNT(*) FROM player_achievements pa
                JOIN achievements a ON a.id = pa.achievement_id
                WHERE pa.player_id = ? AND a.code = ?
                """, Integer.class, playerId, achievementCode);
        if (owned == null || owned == 0) return false;
        Integer issued = jdbc.queryForObject("""
                SELECT COUNT(*) FROM player_equipment_grant_receipts
                WHERE player_id = ? AND entitlement_code = ?
                """, Integer.class, playerId, entitlement.entitlementCode);
        if (issued != null && issued > 0) return false;
        // The Player lock orders contenders; the primary key is the durable final guard.
        jdbc.update("""
                INSERT INTO player_equipment_grant_receipts
                    (player_id, entitlement_code, item_code, definition_version, granted_at)
                VALUES (?, ?, ?, 1, ?)
                """, playerId, entitlement.entitlementCode, entitlement.itemCode,
                LocalDateTime.ofInstant(clock.instant(), DATABASE_ZONE));
        delivery.deliverEquipment(playerId, entitlement.itemCode);
        return true;
    }

    private record Equipment(String entitlementCode, String itemCode) {
        static Equipment forAchievement(String code) {
            return switch (code) {
                case "ACH_FIRST_LIFELOG" -> new Equipment("EQUIPMENT_RECORD_KEEPER_HAT", "IT_RECORD_KEEPER_HAT");
                case "ACH_ROUTE_BACKEND_START" -> new Equipment("EQUIPMENT_DEVELOPER_HOODIE", "IT_DEVELOPER_HOODIE");
                default -> null;
            };
        }
    }
}
