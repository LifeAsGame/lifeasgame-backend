package online.lifeasgame.demo.api;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.demo.application.DemoRunCoordinator;
import online.lifeasgame.demo.application.DemoRunStore;
import online.lifeasgame.demo.domain.DemoError;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/demo")
public class DemoRunController {
    private final DemoRunStore store;
    private final DemoRunCoordinator coordinator;

    private ResponseCookie cookie(String name, String value, Duration age) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(false).sameSite("Strict")
                .path("/api/v1/demo").maxAge(age).build();
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<ApiResponse<DemoRunCoordinator.Bootstrap>> bootstrap() {
        coordinator.requireEnabled();
        DemoRunStore.Proof proof = store.issueProof(
                DemoRunCoordinator.randomToken(), DemoRunCoordinator.randomToken());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                        cookie("portfolio_demo_proof", proof.cookie(), Duration.ofMinutes(15)).toString())
                .body(ApiResponse.onSuccess(new DemoRunCoordinator.Bootstrap(
                        "portfolio-v1", proof.header(), proof.expiresAt())));
    }

    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<DemoRunCoordinator.RunView>> start(
            @CookieValue(value = "portfolio_demo_proof", required = false) String proofCookie,
            @CookieValue(value = "portfolio_demo_manager", required = false) String previousManager,
            @RequestHeader(value = "X-Demo-Proof", required = false) String proofHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        DemoRunCoordinator.Started started = coordinator.start(proofCookie, proofHeader, key, previousManager);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                        cookie("portfolio_demo_manager", started.managerToken(),
                                Duration.between(Instant.now(), started.run().expiresAt())).toString())
                .body(ApiResponse.onSuccess(coordinator.view(started.run())));
    }

    @GetMapping("/runs/current")
    public ResponseEntity<ApiResponse<DemoRunCoordinator.RunView>> current(
            @CookieValue(value = "portfolio_demo_manager", required = false) String manager) {
        return ApiResponses.ok(coordinator.view(coordinator.managed(manager)));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ApiResponse<DemoRunCoordinator.RunView>> byId(
            @CookieValue(value = "portfolio_demo_manager", required = false) String manager,
            @PathVariable String runId) {
        DemoRunStore.Run run = coordinator.managed(manager);
        if (!run.id().equals(runId)) throw new DomainException(DemoError.RUN_NOT_FOUND);
        return ApiResponses.ok(coordinator.view(run));
    }

    @PostMapping("/runs/current/retry")
    public ResponseEntity<ApiResponse<DemoRunCoordinator.RunView>> retry(
            @CookieValue(value = "portfolio_demo_manager", required = false) String manager) {
        DemoRunStore.Run run = coordinator.managed(manager);
        coordinator.retry(run.id());
        return ApiResponses.ok(coordinator.view(store.byId(run.id())));
    }

    @PostMapping("/runs/current/actors/{actor}/activate")
    public ResponseEntity<ApiResponse<DemoRunCoordinator.ActorTokens>> activate(
            @CookieValue(value = "portfolio_demo_manager", required = false) String manager,
            @PathVariable String actor) {
        return ApiResponses.ok(coordinator.activate(coordinator.managed(manager).id(), actor));
    }

    @PostMapping("/runs/current/peer-links")
    public ResponseEntity<ApiResponse<Map<String, Object>>> peerLink(
            @CookieValue(value = "portfolio_demo_manager", required = false) String manager) {
        String code = coordinator.peerLink(coordinator.managed(manager).id());
        return ApiResponses.ok(Map.of("code", code, "expiresAt", Instant.now().plusSeconds(300)));
    }

    public record Redeem(String code) {}

    @PostMapping("/peer-links/redeem")
    public ResponseEntity<ApiResponse<DemoRunCoordinator.ActorTokens>> redeem(@RequestBody Redeem request) {
        return ApiResponses.ok(coordinator.redeem(request.code()));
    }

    @PostMapping("/runs/current/close")
    public ResponseEntity<ApiResponse<Void>> close(
            @CookieValue(value = "portfolio_demo_manager", required = false) String manager) {
        coordinator.close(coordinator.managed(manager).id());
        return ApiResponses.noContent();
    }
}
