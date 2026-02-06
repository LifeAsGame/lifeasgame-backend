package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminTitleWebMapper;
import online.lifeasgame.character.api.admin.request.AdminTitleRequest;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse;
import online.lifeasgame.character.api.admin.spec.AdminTitleApiSpecV1;
import online.lifeasgame.character.application.TitleService;
import online.lifeasgame.character.application.result.TitleResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/titles")
public class AdminTitleController implements AdminTitleApiSpecV1 {

    private final TitleService titleService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminTitleResponse.Info>> create(
            @Valid @RequestBody AdminTitleRequest.Create request
    ) {
        TitleResult.Info result = titleService.create(AdminTitleWebMapper.toCreateCommand(request));
        return ApiResponses.ok(AdminTitleWebMapper.toInfo(result));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AdminTitleResponse.Infos>> list(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<TitleResult.Info> results = titleService.getTitles(categories);
        return ApiResponses.ok(AdminTitleWebMapper.toInfos(results));
    }
}
