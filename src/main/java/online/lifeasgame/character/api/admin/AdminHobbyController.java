package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminHobbyWebMapper;
import online.lifeasgame.character.api.admin.request.AdminHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminHobbyResponse;
import online.lifeasgame.character.api.admin.spec.AdminHobbyApiSpecV1;
import online.lifeasgame.character.application.HobbyService;
import online.lifeasgame.character.application.result.HobbyResult;
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
@RequestMapping("/admin/v1/hobbies")
public class AdminHobbyController implements AdminHobbyApiSpecV1 {

    private final HobbyService adminHobbyService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminHobbyResponse.HobbyInfo>> create(
            @Valid @RequestBody AdminHobbyRequest.CreateHobby request
    ) {
        HobbyResult.HobbyInfo hobbyInfo = adminHobbyService.create(AdminHobbyWebMapper.toCommand(request));
        return ApiResponses.created(
                URI.create("/admin/v1/hobbies/"),
                AdminHobbyWebMapper.toHobbyInfo(hobbyInfo)
        );
    }
}
