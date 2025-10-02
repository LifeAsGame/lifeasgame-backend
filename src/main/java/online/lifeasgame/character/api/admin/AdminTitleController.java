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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/titles")
public class AdminTitleController implements AdminTitleApiSpecV1 {

    private final TitleService adminTitleService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminTitleResponse.TitleInfo>> create(
            @Valid @RequestBody AdminTitleRequest.CreateTitle request
    ) {
        TitleResult.TitleInfo titleInfo = adminTitleService.create(AdminTitleWebMapper.toCommand(request));
        return ApiResponses.ok(
                AdminTitleWebMapper.toTitleInfo(titleInfo)
        );
    }
}
