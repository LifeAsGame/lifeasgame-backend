package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.character.domain.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerLookupService implements PlayerLookupApi {

    private final PlayerReader playerReader;

    @Override
    public Long findPlayerIdByUserId(Long userId) {
        Player player = playerReader.getByUserId(userId);
        return player == null ? null : player.getId();
    }
}
