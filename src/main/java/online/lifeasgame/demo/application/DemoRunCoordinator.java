package online.lifeasgame.demo.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.auth.application.AuthService;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.demo.domain.DemoError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DemoRunCoordinator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final DemoRunStore store;
    private final DemoRunProvisioner provisioner;
    private final AuthService auth;
    private final DemoProperties properties;
    @Value("${lifeasgame.jwt.secret}")
    private String secret;

    public record Bootstrap(String templateVersion, String proof, Instant expiresAt) {}
    public record Started(DemoRunStore.Run run, String managerToken) {}
    public record ActorTokens(String runId, String actor, String accessToken, String refreshToken,
                              Long userId, Long playerId) {}
    public record RunView(String runId, String templateVersion, String status, String failureCode,
                          Instant createdAt, Instant expiresAt, List<String> actors,
                          Map<String, Object> scenarios) {}

    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String managerToken(String proofId, String attemptKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(("portfolio-demo-manager:" + proofId + ":" + attemptKey)
                            .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public void requireEnabled() {
        if (!properties.enabled()) throw new DomainException(DemoError.DISABLED);
    }

    public Started start(String proofCookie, String proofHeader, String key, String previousManager) {
        requireEnabled();
        String proofId = store.proofId(proofCookie, proofHeader);
        String manager = managerToken(proofId, key);
        DemoRunStore.Run run = store.start(proofId, key, manager);
        if (previousManager != null) {
            try {
                DemoRunStore.Run previous = store.byManager(previousManager);
                if (!previous.id().equals(run.id())) store.close(previous.id());
            } catch (DomainException ignored) {
                // An invalid previous browser cookie never grants authority over this run.
            }
        }
        if (run.status().equals("PROVISIONING")) provisioner.schedule(run.id());
        return new Started(run, manager);
    }

    public DemoRunStore.Run managed(String manager) {
        requireEnabled();
        DemoRunStore.Run run = store.byManager(manager);
        if (run.status().equals("PROVISIONING")) provisioner.schedule(run.id());
        return run;
    }

    public RunView view(DemoRunStore.Run run) {
        DemoRunStore.Targets targets = store.targets(run.id());
        return new RunView(run.id(), "portfolio-v1", run.status(), run.failureCode(),
                run.createdAt(), run.expiresAt(), List.of("explorer", "seller", "journey"),
                Map.of(
                        "marketplace", Map.of("buyer", "explorer", "seller", "seller",
                                "itemCode", "IT_RECORD_CRYSTAL", "price", 25, "quantity", 1,
                                "listingId", targets == null ? 0 : targets.listingId()),
                        "recordReward", Map.of("actor", "explorer", "firstQuestCode", "Q_RECORD_FIRST_TRACE",
                                "mailQuestCode", "Q_ADVENTURE_PREPARATION", "recordCountForMail", 3),
                        "journey", Map.of("actor", "journey", "routeCode", "ROUTE_BACKEND_DEVELOPER_START",
                                "firstQuestCode", "Q_DEV_DEFINE_BACKEND_GOAL",
                                "roleId", targets == null ? 0 : targets.roleId(),
                                "projectLifeLogId", targets == null ? 0 : targets.projectLifeLogId()),
                        "chat", Map.of("actorA", "explorer", "actorB", "seller",
                                "channelId", targets == null ? 0 : targets.channelId())));
    }

    private DemoRunStore.Run ready(String runId) {
        DemoRunStore.Run run = store.byId(runId);
        if (run.status().equals("EXPIRED") || run.status().equals("CLOSED")) {
            throw new DomainException(DemoError.RUN_EXPIRED);
        }
        if (!run.status().equals("READY")) throw new DomainException(DemoError.NOT_READY);
        return run;
    }

    public ActorTokens activate(String runId, String actor) {
        requireEnabled();
        ready(runId);
        if (!List.of("explorer", "seller", "journey").contains(actor)) {
            throw new DomainException(DemoError.ACTOR_FORBIDDEN);
        }
        DemoRunStore.Actor member = store.actor(runId, actor);
        AuthResult.TokenPair pair = auth.issueToken(member.userId(), member.playerId());
        return new ActorTokens(runId, actor, pair.accessToken(), pair.refreshToken(),
                member.userId(), member.playerId());
    }

    public void retry(String runId) {
        requireEnabled();
        if (!store.retry(runId)) throw new DomainException(DemoError.NOT_READY);
        provisioner.schedule(runId);
    }

    public void close(String runId) {
        requireEnabled();
        store.close(runId);
    }

    public String peerLink(String runId) {
        requireEnabled();
        ready(runId);
        String code = randomToken();
        return store.issuePeerLink(runId, code);
    }

    public ActorTokens redeem(String code) {
        requireEnabled();
        return activate(store.redeemPeerLink(code), "seller");
    }
}
