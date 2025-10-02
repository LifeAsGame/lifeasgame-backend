package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.TitleCommand;
import online.lifeasgame.character.application.result.TitleResult;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TitleService {

    private final TitleReader titleReader;
    private final TitleWriter titleWriter;

    public List<TitleResult.TitleInfo> getTitles(List<String> categories) {
        List<Title> titles = titleReader.getTitles(TitleCategory.parse(categories));
        return TitleResult.TitleInfo.fromList(titles);
    }

    @Transactional
    public TitleResult.TitleInfo create(TitleCommand.CreateTitle command) {
        Title title = titleWriter.create(
                Title.of(
                        command.code(),
                        command.name(),
                        TitleCategory.parse(command.category()),
                        command.descMd()
                )
        );

        return TitleResult.TitleInfo.of(
                title.getCode(),
                title.getName(),
                title.getCategory().name(),
                title.getDescMd()
        );
    }
}
