package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.Title;

import java.util.List;
import java.util.stream.Collectors;

public class TitleResult {

    private TitleResult() {
    }

    public record Info(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static Info from(Title title) {
            return new Info(
                    title.getCode(),
                    title.getName(),
                    title.getCategory().name(),
                    title.getDescMd()
            );
        }

        public static List<Info> fromList(List<Title> titles) {
            return titles.stream().map(Info::from).collect(Collectors.toList());
        }

        public static Info of(String code, String name, String category, String descMd) {
            return new Info(code, name, category, descMd);
        }
    }
}
