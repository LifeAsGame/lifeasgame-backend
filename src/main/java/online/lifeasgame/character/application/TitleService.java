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

    public List<TitleResult.Info> getTitles(List<String> categories) {
        List<Title> titles = titleReader.getByCategories(TitleCategory.parse(categories));
        return TitleResult.Info.fromList(titles);
    }

    @Transactional
    public TitleResult.Info create(TitleCommand.Create command) {
        Title title = titleWriter.create(
                Title.create(
                        command.code(),
                        command.name(),
                        TitleCategory.parse(command.category()),
                        command.descMd()
                )
        );

        return TitleResult.Info.from(title);
    }

    public TitleResult.Info getTitle(Long titleId) {
        Title title = titleReader.getByIdOrThrow(titleId);
        return TitleResult.Info.from(title);
    }
}
