package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.Title;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerTitleService {

    private final PlayerTitleReader playerTitleReader;
    private final PlayerTitleWriter playerTitleWriter;
    private final TitleReader titleReader;

    public List<PlayerTitleResult.Info> getPlayerTitleInfos(Long playerId) {
        List<PlayerTitleView> playerTitleViews = playerTitleReader.getPlayerTitleInfos(playerId);
        return playerTitleViews.stream()
                .map(PlayerTitleResult.Info::from)
                .toList();
    }

    @Transactional
    public PlayerTitleResult.Granted grantTitle(Long playerId, Long titleId) {
        Title title = titleReader.getTitle(titleId);

        PlayerTitle saved = playerTitleWriter.grantTitle(
                PlayerTitle.create(
                        playerId,
                        titleId
                )
        );

        return PlayerTitleResult.Granted.of(
                saved.getPlayerId(),
                saved.getTitleId(),
                title.getCode(),
                title.getName(),
                title.getCategory().name(),
                saved.getAcquiredAt()
        );
    }
}
