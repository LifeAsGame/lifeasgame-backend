package online.lifeasgame.character.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerCertificationFacade;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.presentation.mapper.PlayerCertificationWebMapper;
import online.lifeasgame.character.presentation.response.PlayerCertificationResponse;
import online.lifeasgame.character.presentation.spec.PlayerCertificationApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerCertificationController implements PlayerCertificationApiSpecV1 {

    private final PlayerCertificationFacade playerCertificationFacade;

    @Override
    @GetMapping("/certifications")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.PlayerCertificationInfos>> playerCertificationInfos() {
        List<PlayerCertificationResult.PlayerCertificationInfo> playerCertificationInfos = playerCertificationFacade.getPlayerCertificationInfos();

        return ApiResponses.ok(
                PlayerCertificationWebMapper.toPlayerCertificationInfos(playerCertificationInfos)
        );
    }
}
