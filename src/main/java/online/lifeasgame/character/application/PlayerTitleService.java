package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
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
    private final PlayerReader playerReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional(readOnly = true)
    public List<PlayerTitleResult.Info> getPlayerTitleInfos() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<PlayerTitleView> playerTitleViews = playerTitleReader.getViewsByPlayerId(playerId);
        return playerTitleViews.stream()
                .map(PlayerTitleResult.Info::from)
                .toList();
    }

    @Transactional
    public PlayerTitleResult.Created createTitle(Long playerId, Long titleId) {
        Title title = titleReader.getByIdOrThrow(titleId);

        PlayerTitle playerTitle = playerTitleWriter.create(
                PlayerTitle.create(playerId, titleId)
        );

        return PlayerTitleResult.Created.from(playerTitle, title);
    }

    @Transactional
    public PlayerTitleResult.Revoked revokeTitle(Long playerId, Long titleId) {
        var player = playerReader.getByIdForUpdateOrThrow(playerId);
        playerTitleReader.assertHasTitle(playerId, titleId);
        player.clearRepresentativeTitleIfMatches(titleId);
        playerTitleWriter.revoke(playerId, titleId);
        return new PlayerTitleResult.Revoked(playerId, titleId);
    }
}
