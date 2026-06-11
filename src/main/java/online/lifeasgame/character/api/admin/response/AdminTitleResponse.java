package online.lifeasgame.character.api.admin.response;

import java.util.List;

public final class AdminTitleResponse {

    private AdminTitleResponse() {
    }

    public record Info(
            Long titleId,
            String code,
            String name,
            String category,
            String description
    ) {
    }

    public record Deleted(Long titleId) {}

    public record Infos(List<Info> infos) {
    }
}
