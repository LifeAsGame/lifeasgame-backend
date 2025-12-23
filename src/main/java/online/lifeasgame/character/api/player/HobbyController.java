package online.lifeasgame.character.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.HobbyWebMapper;
import online.lifeasgame.character.api.player.response.HobbyResponse;
import online.lifeasgame.character.api.player.spec.HobbyApiSpecV1;
import online.lifeasgame.character.application.HobbyService;
import online.lifeasgame.character.application.result.HobbyResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hobbies")
public class HobbyController implements HobbyApiSpecV1 {

    private final HobbyService hobbyService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<HobbyResponse.Infos>> HobbyInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<HobbyResult.Info> results = hobbyService.getHobbies(categories);
        return ApiResponses.ok(HobbyWebMapper.toInfos(results));
    }
}
