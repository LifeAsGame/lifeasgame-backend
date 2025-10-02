package online.lifeasgame.character.api.player.response;

import java.util.List;

public class EquipmentSlotResponse {

    private EquipmentSlotResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
    }

    public record Info(
            String code,
            String name,
            String category,
            String role
    ) {
        public static Info of(String code, String name, String category, String role) {
            return new Info(code, name, category, role);
        }
    }
}
