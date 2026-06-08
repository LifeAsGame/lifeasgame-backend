package online.lifeasgame.character.api.player.response;

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
