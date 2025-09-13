package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AdminTitleCommand;
import online.lifeasgame.character.application.result.AdminTitleResult;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class AdminTitleService {

    private final TitleWriter titleWriter;

    @Transactional
    public AdminTitleResult.TitleInfo create(AdminTitleCommand.CreateTitle command) {
        Title title = titleWriter.create(
                Title.of(
                        command.code(),
                        command.name(),
                        TitleCategory.parse(command.category()),
                        command.descMd()
                )
        );

        return AdminTitleResult.TitleInfo.of(
                title.getCode(),
                title.getName(),
                title.getCategory().name(),
                title.getDescMd()
        );
    }
}
