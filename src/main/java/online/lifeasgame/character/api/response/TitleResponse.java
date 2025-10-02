package online.lifeasgame.character.api.response;

import java.util.List;

public class TitleResponse {

    private TitleResponse() {
    }

    public record TitleInfos(
            List<TitleInfo> titleInfos
    ) {
        public static TitleInfos of(List<TitleInfo> titleInfos) {
            return new TitleInfos(titleInfos);
        }
    }

    public record TitleInfo(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static TitleInfo of(String code, String name, String category, String descMd) {
            return new TitleInfo(code, name, category, descMd);
        }
    }
}
