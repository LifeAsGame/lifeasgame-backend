package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.TitleResult;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TitleService {

    private final TitleReader titleReader;

    public List<TitleResult.TitleInfo> getTitles(List<String> categories) {
        List<Title> titles = titleReader.getTitles(TitleCategory.parse(categories));
        return TitleResult.TitleInfo.fromList(titles);
    }
}
