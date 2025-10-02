package online.lifeasgame.character.api;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminPlayerTitleService;
import online.lifeasgame.character.application.result.AdminPlayerTitleResult;
import online.lifeasgame.character.api.mapper.AdminPlayerTitleWebMapper;
import online.lifeasgame.character.api.response.AdminPlayerTitleResponse;
import online.lifeasgame.character.api.spec.AdminPlayerTitleApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerTitleController implements AdminPlayerTitleApiSpecV1 {

    private final AdminPlayerTitleService adminPlayerTitleService;

    @Override
    @PostMapping("/{playerId}/titles/{titleId}")
    public ResponseEntity<ApiResponse<AdminPlayerTitleResponse.GrantedTitle>> grantTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    ) {
        AdminPlayerTitleResult.GrantedTitle grantedTitle = adminPlayerTitleService.grantTitle(playerId, titleId);

        return ApiResponses.ok(
                AdminPlayerTitleWebMapper.toGrantedTitle(grantedTitle)
        );
    }
}
