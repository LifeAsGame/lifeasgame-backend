package online.lifeasgame.character.api.player.response;

import io.swagger.v3.oas.annotations.media.Schema;

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
                @Schema(description = "누적 경험치. 현재 레벨 내 경험치와 구분합니다.")
                long exp,
                int str,
                int agi,
                int dex,
                int intel,
                int vit,
                int luc,
                Map<String, Integer> extraStats,
                Long representativeTitleId,
                @Schema(description = "현재 레벨에서 획득한 경험치. 최고 레벨에서는 0.", minimum = "0")
                long expIntoLevel,
                @Schema(description = "현재 레벨을 완료하는 데 필요한 전체 경험치. 진행 표기의 분모이며 최고 레벨에서는 0.", minimum = "0")
                long capForLevel,
                @Schema(description = "다음 레벨까지 남은 경험치. 최고 레벨에서는 0.", minimum = "0")
                long expToNext,
                @Schema(description = "레벨 진행률. 최고 레벨에서는 1.0.", minimum = "0", maximum = "1")
                double progressRatio,
                @Schema(description = "현재 레벨이 레벨 정책의 최고 레벨 이상인지 여부. true이면 경험치 분모로 나누지 않습니다.")
                boolean maxLevelReached
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
