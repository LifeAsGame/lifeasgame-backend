package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.character.application.view.PlayerTitleView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerTitleService {

    private final PlayerTitleReader playerTitleReader;

    public List<PlayerTitleResult.PlayerTitleInfo> getPlayerTitleInfos(Long playerId) {
        List<PlayerTitleView> playerTitleViews = playerTitleReader.getPlayerTitleInfos(playerId);
        return playerTitleViews.stream()
                .map(PlayerTitleResult.PlayerTitleInfo::from)
                .toList();
    }
}
