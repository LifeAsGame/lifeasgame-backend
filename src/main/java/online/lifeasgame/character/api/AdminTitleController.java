package online.lifeasgame.character.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminTitleService;
import online.lifeasgame.character.application.result.AdminTitleResult;
import online.lifeasgame.character.api.mapper.AdminTitleWebMapper;
import online.lifeasgame.character.api.request.AdminTitleRequest;
import online.lifeasgame.character.api.response.AdminTitleResponse;
import online.lifeasgame.character.api.spec.AdminTitleApiSpecV1;
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

    private final AdminTitleService adminTitleService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminTitleResponse.TitleInfo>> create(
            @Valid @RequestBody AdminTitleRequest.CreateTitle request
    ) {
        AdminTitleResult.TitleInfo titleInfo = adminTitleService.create(AdminTitleWebMapper.toCommand(request));
        return ApiResponses.ok(
                AdminTitleWebMapper.toTitleInfo(titleInfo)
        );
    }
}
