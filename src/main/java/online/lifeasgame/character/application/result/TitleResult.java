package online.lifeasgame.character.application.result;

import java.util.List;
import java.util.stream.Collectors;
import online.lifeasgame.character.domain.Title;

public class TitleResult {

    private TitleResult() {
    }

    public record TitleInfo(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static TitleResult.TitleInfo from(Title title) {
            return new TitleResult.TitleInfo(
                    title.getCode(),
                    title.getName(),
                    title.getCategory().name(),
                    title.getDescMd()
            );
        }

        public static List<TitleInfo> fromList(List<Title> titles) {
            return titles.stream().map(TitleResult.TitleInfo::from).collect(Collectors.toList());
        }
    }
}
