package online.lifeasgame.character.api.player.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class PlayerResponse {

    private PlayerResponse() {
    }

    public record CreatedWithToken(Long id, String accessToken, String refreshToken) {
    }

    public record Info(
            Long playerId,
            String name,
            String gender,
            String job,
            int level,
            long exp,
            int currentHealth,
            int healthCapacity,
            int currentMana,
            int manaCapacity,
            int str, int agi, int dex, int intel, int vit, int luc,
            Map<String, Integer> extraStats,
            List<StatusEffects> effects,
            Long representativeTitleId
    ) {
        public record StatusEffects(String code, String effect) {
        }
    }

    public record UpdatedTitle(Long titleId) {
    }

    public record Growth(Current current, List<RecentExpChange> recentExpChanges) {
        public record Current(
                int level,
                long exp,
                int str,
                int agi,
                int dex,
                int intel,
                int vit,
                int luc,
                Map<String, Integer> extraStats,
                Long representativeTitleId
        ) {
        }

        public record RecentExpChange(
                Long changeId,
                long requestedExp,
                long appliedExp,
                long leftoverExp,
                int beforeLevel,
                int afterLevel,
                long beforeTotalExp,
                long afterTotalExp,
                Instant occurredAt,
                String sourceType,
                Long sourceId
        ) {
        }
    }

    public record CharacterSheet(
            Info player,
            RepresentativeTitle title,
            List<EquipmentView> equipments
    ) {
        public record RepresentativeTitle(
                Long titleId,
                String code,
                String name,
                String category
        ) {}

        public record EquipmentView(
                Long slotId,
                String slotCode,
                String slotName,
                String slotCategory,
                String slotRole,
                Long itemInstanceId
        ) {}
    }
}
