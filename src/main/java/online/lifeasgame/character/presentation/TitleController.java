package online.lifeasgame.character.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.TitleService;
import online.lifeasgame.character.application.result.TitleResult;
import online.lifeasgame.character.presentation.mapper.TitleWebMapper;
import online.lifeasgame.character.presentation.response.TitleResponse.TitleInfos;
import online.lifeasgame.character.presentation.spec.TitleApiSpecV1;
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
    public ResponseEntity<ApiResponse<TitleInfos>> titleInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<TitleResult.TitleInfo> titleList = titleService.getTitleList(categories);
        return ApiResponses.ok(
                TitleWebMapper.toTitleList(titleList)
        );
    }
}
