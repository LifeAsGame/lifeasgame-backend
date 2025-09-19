package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.AdminPlayerTitleResult;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.Title;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPlayerTitleService {

    private final PlayerTitleWriter playerTitleWriter;
    private final TitleReader titleReader;

    @Transactional
    public AdminPlayerTitleResult.GrantedTitle grantTitle(Long playerId, Long titleId) {
        Title title = titleReader.getTitle(titleId);

        PlayerTitle saved = playerTitleWriter.grantTitle(
                PlayerTitle.create(
                        playerId,
                        titleId
                )
        );

        return AdminPlayerTitleResult.GrantedTitle.of(
                saved.getPlayerId(),
                saved.getTitleId(),
                title.getCode(),
                title.getName(),
                title.getCategory().name(),
                saved.getAcquiredAt()
        );
    }
}
