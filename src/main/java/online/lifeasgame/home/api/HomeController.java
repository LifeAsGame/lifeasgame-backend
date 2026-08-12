package online.lifeasgame.home.api;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.home.api.mapper.HomeWebMapper;
import online.lifeasgame.home.api.response.HomeResponse;
import online.lifeasgame.home.api.spec.HomeApiSpecV1;
import online.lifeasgame.home.application.HomeQueryService;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/home")
public class HomeController implements HomeApiSpecV1 {

    private final HomeQueryService homeQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<HomeResponse.Summary>> home() {
        return ApiResponses.ok(HomeWebMapper.toSummary(
                homeQueryService.home()
        ));
    }
}
