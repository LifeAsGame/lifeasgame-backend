package online.lifeasgame.character.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.HobbyService;
import online.lifeasgame.character.application.result.HobbyResult.HobbyInfo;
import online.lifeasgame.character.presentation.mapper.HobbyWebMapper;
import online.lifeasgame.character.presentation.response.HobbyResponse;
import online.lifeasgame.character.presentation.spec.HobbyApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hobbies")
public class HobbyController implements HobbyApiSpecV1 {

    private final HobbyService hobbyService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<HobbyResponse.HobbyInfos>> HobbyInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<HobbyInfo> HobbyInfos = hobbyService.getHobbies(categories);
        return ApiResponses.ok(
                HobbyWebMapper.toHobbyInfos(HobbyInfos)
        );
    }
}
