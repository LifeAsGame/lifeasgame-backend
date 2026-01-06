package online.lifeasgame.character.api.admin.response;

import java.util.List;

public final class AdminCertificationResponse {

    private AdminCertificationResponse() {
    }

    public record Info(
            Long certificationId,
            String name,
            String issuer,
            String category
    ) {
    }

    public record Deleted(Long certificationId) {}

    public record Infos(List<Info> infos) {
    }
}
