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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hobbies")
public class HobbyController implements HobbyApiSpecV1 {

    private final HobbyService hobbyService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<HobbyResponse.Infos>> hobbyInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<HobbyResult.Info> results = hobbyService.getHobbies(categories);
        return ApiResponses.ok(HobbyWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{hobbyId}")
    public ResponseEntity<ApiResponse<HobbyResponse.Info>> hobbyInfo(
            @PathVariable Long hobbyId
    ) {
        HobbyResult.Info result = hobbyService.getHobby(hobbyId);
        return ApiResponses.ok(HobbyWebMapper.toInfo(result));
    }
}
