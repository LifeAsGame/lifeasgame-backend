package online.lifeasgame.character.api.player;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.TitleService;
import online.lifeasgame.character.application.result.TitleResult;
import online.lifeasgame.character.api.player.mapper.TitleWebMapper;
import online.lifeasgame.character.api.player.response.TitleResponse;
import online.lifeasgame.character.api.player.spec.TitleApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/titles")
public class TitleController implements TitleApiSpecV1 {

    private final TitleService titleService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<TitleResponse.TitleInfos>> titleInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<TitleResult.TitleInfo> titleInfos = titleService.getTitles(categories);
        return ApiResponses.ok(
                TitleWebMapper.toTitleInfos(titleInfos)
        );
    }
}
