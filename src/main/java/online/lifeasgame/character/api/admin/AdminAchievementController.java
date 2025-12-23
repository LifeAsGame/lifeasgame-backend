package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminAchievementWebMapper;
import online.lifeasgame.character.api.admin.request.AdminAchievementRequest;
import online.lifeasgame.character.api.admin.response.AdminAchievementResponse;
import online.lifeasgame.character.api.admin.spec.AdminAchievementApiSpecV1;
import online.lifeasgame.character.application.AchievementService;
import online.lifeasgame.character.application.result.AchievementResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/achievements")
public class AdminAchievementController implements AdminAchievementApiSpecV1 {

    private final AchievementService achievementService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminAchievementResponse.Info>> create(
            @Valid @RequestBody AdminAchievementRequest.Create request
    ) {
        AchievementResult.Info result = achievementService.create(AdminAchievementWebMapper.toCreateCommand(request));

        return ApiResponses.created(
                URI.create("/admin/v1/achievements/" + result.code()),
                AdminAchievementWebMapper.toInfo(result)
        );
    }
}
