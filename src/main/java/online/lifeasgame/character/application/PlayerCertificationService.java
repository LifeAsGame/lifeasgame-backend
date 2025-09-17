package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerCertificationService {

    private final PlayerCertificationReader playerCertificationReader;

    public List<PlayerCertificationResult.PlayerCertificationInfo> getPlayerCertificationInfos(Long playerId) {
        List<PlayerCertificationView> playerCertificationViews = playerCertificationReader.getPlayerCertificationInfos(playerId);
        return playerCertificationViews.stream()
                .map(PlayerCertificationResult.PlayerCertificationInfo::from)
                .toList();
    }
}
