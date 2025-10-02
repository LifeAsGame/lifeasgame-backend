package online.lifeasgame.character.api;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminAchievementService;
import online.lifeasgame.character.application.result.AdminAchievementResult;
import online.lifeasgame.character.api.mapper.AdminAchievementWebMapper;
import online.lifeasgame.character.api.request.AdminAchievementRequest;
import online.lifeasgame.character.api.response.AdminAchievementResponse;
import online.lifeasgame.character.api.spec.AdminAchievementApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/achievements")
public class AdminAchievementController implements AdminAchievementApiSpecV1 {

    private final AdminAchievementService adminAchievementService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminAchievementResponse.AchievementInfo>> create(
            @Valid @RequestBody AdminAchievementRequest.CreateAchievement request
    ) {
        AdminAchievementResult.AchievementInfo achievementInfo = adminAchievementService.create(AdminAchievementWebMapper.toCommand(request));
        return ApiResponses.created(
                URI.create("/admin/v1/achievements/" + achievementInfo.code()),
                AdminAchievementWebMapper.toAchievementInfo(achievementInfo)
        );
    }
}
