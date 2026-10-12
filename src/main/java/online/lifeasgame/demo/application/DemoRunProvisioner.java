package online.lifeasgame.demo.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.economy.application.internal.PortfolioDemoBalanceApi;
import online.lifeasgame.inventory.application.internal.InventoryContainerProvisioningApi;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.user.application.internal.UserAuthApi;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.user.domain.error.UserError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DemoRunProvisioner {
    private static final Logger log = LoggerFactory.getLogger(DemoRunProvisioner.class);
    private final DemoRunStore store;
    private final UserAuthApi users;
    private final PlayerLookupApi players;
    private final PortfolioDemoBalanceApi balance;
    private final InventoryContainerProvisioningApi inventoryContainers;
    private final JwtProvider jwt;
    private final ObjectMapper json;
    private final TaskExecutor taskExecutor;
    @org.springframework.beans.factory.annotation.Value("${lifeasgame.jwt.secret}")
    private String secret;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public void schedule(String runId) {
        if (!store.claimProvisioning(runId)) return;
        taskExecutor.execute(() -> {
            try {
                prepare(runId);
                if (store.byId(runId).status().equals("PROVISIONING")) store.markReady(runId);
            } catch (Exception failure) {
                // Never log credentials, JWTs, request bodies or one-time codes.
                log.warn("Portfolio demo preparation failed runId={} type={}", runId, failure.getClass().getSimpleName());
                store.markFailed(runId);
            }
        });
    }

    private String password(String runId, String actor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(("portfolio-demo:" + runId + ":" + actor).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private DemoRunStore.Actor account(String runId, String actor) {
        String email = "portfolio-" + runId + "-" + actor + "@example.invalid";
        String password = password(runId, actor);
        Long userId;
        try {
            userId = users.authenticate(email, password);
        } catch (DomainException missing) {
            if (missing.getErrorCode() != UserError.USER_NOT_FOUND) throw missing;
            userId = users.register(email, password, "p" + runId.replace("-", "").substring(0, 8) + actor);
        }
        Long playerId = players.findPlayerIdByUserId(userId);
        store.bindActor(runId, actor, userId, playerId);
        if (playerId == null) {
            call(userId, null, "POST", "/api/v1/players/register",
                    Map.of("name", "체험 " + actor, "gender", "MALE"), null);
            playerId = players.findPlayerIdByUserId(userId);
            if (playerId == null) throw new IllegalStateException("Player registration incomplete");
            store.bindActor(runId, actor, userId, playerId);
        }
        return new DemoRunStore.Actor(runId, actor, userId, playerId);
    }

    private JsonNode call(DemoRunStore.Actor actor, String method, String path, Object body, String key) {
        return call(actor.userId(), actor.playerId(), method, path, body, key);
    }

    private JsonNode call(Long userId, Long playerId, String method, String path, Object body, String key) {
        try {
            String value = body == null ? "" : json.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080" + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + jwt.createAccessToken(userId, playerId, true))
                    .header("Content-Type", "application/json");
            if (key != null) builder.header("Idempotency-Key", key);
            HttpResponse<String> response = http.send(builder.method(method,
                    body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(value)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Seed API rejected " + method + " " + path + " HTTP " + response.statusCode());
            }
            if (response.body().isEmpty()) return json.nullNode();
            JsonNode root = json.readTree(response.body());
            return root.has("result") ? root.path("result") : root;
        } catch (Exception exception) {
            throw new IllegalStateException("Seed API invocation failed", exception);
        }
    }

    private void accept(DemoRunStore.Actor actor, String code) {
        String path = "/api/v1/players/quests/" + code;
        if (call(actor, "GET", path, null, null).path("acceptance").isNull()) {
            call(actor, "POST", path, Map.of(), null);
        }
    }

    private void trace(DemoRunStore.Actor actor, String runId, int index) {
        call(actor, "POST", "/api/v1/lifelogs/quick-record", Map.of(
                "type", "COLLECTION", "lifeLogSubtype", "QUICK_NOTE",
                "collection", Map.of("category", "OTHER", "title", "체험 판매 준비 " + index, "quantity", 1)),
                "portfolio-demo-" + runId + "-seller-trace-" + index);
    }

    private JsonNode await(String description, java.util.function.Supplier<JsonNode> read,
                           java.util.function.Predicate<JsonNode> done) {
        for (int attempt = 0; attempt < 90; attempt++) {
            JsonNode result = read.get();
            if (done.test(result)) return result;
            try { Thread.sleep(1000); }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(description + " interrupted");
            }
        }
        throw new IllegalStateException(description + " timed out");
    }

    private void prepare(String runId) {
        DemoRunStore.Actor explorer = account(runId, "explorer");
        DemoRunStore.Actor seller = account(runId, "seller");
        DemoRunStore.Actor journey = account(runId, "journey");
        inventoryContainers.ensureContainers(explorer.playerId());
        inventoryContainers.ensureContainers(seller.playerId());
        inventoryContainers.ensureContainers(journey.playerId());
        balance.grantStartingGold(runId, explorer.playerId());
        accept(explorer, "Q_RECORD_FIRST_TRACE");
        accept(explorer, "Q_ADVENTURE_PREPARATION");

        accept(seller, "Q_ADVENTURE_PREPARATION");
        for (int index = 1; index <= 3; index++) trace(seller, runId, index);
        await("seller reward", () -> call(seller, "GET", "/api/v1/mailbox", null, null),
                mailbox -> {
                    for (JsonNode entry : mailbox.path("entries")) if (!entry.path("bound").asBoolean(true)) return true;
                    return false;
                });
        JsonNode inventory = call(seller, "GET", "/api/v1/inventory", null, null);
        if (!hasUnbound(inventory.path("entries"))) {
            for (JsonNode mail : call(seller, "GET", "/api/v1/mailbox", null, null).path("entries")) {
                if (!mail.path("bound").asBoolean(true)) {
                    call(seller, "POST", "/api/v1/mailbox/claim",
                            Map.of("slotIndex", mail.path("slotIndex").asInt(), "quantity", mail.path("quantity").asInt()), null);
                    break;
                }
            }
            inventory = call(seller, "GET", "/api/v1/inventory", null, null);
        }
        JsonNode listings = call(seller, "GET", "/api/v1/economy/listings/me", null, null).path("listings");
        if (listings.isEmpty()) {
            for (JsonNode entry : inventory.path("entries")) {
                if (!entry.path("bound").asBoolean(true)) {
                    call(seller, "POST", "/api/v1/economy/listings", Map.of(
                            "inventoryEntryId", entry.path("itemInstanceId").asLong(), "price", 25, "currency", "GOLD"), null);
                    break;
                }
            }
        }
        listings = call(seller, "GET", "/api/v1/economy/listings/me", null, null).path("listings");
        if (listings.size() != 1 || !listings.get(0).path("status").asText().equals("OPEN") ||
                listings.get(0).path("price").asLong() != 25 ||
                listings.get(0).path("saleQuantity").asInt() != 1) {
            throw new IllegalStateException("Seller listing is not at template start");
        }
        long listingId = listings.get(0).path("id").asLong();

        JsonNode roles = call(journey, "GET", "/api/v1/roles", null, null);
        if (roles.isEmpty()) {
            call(journey, "POST", "/api/v1/roles", Map.of(
                    "roleType", "ROLE_BACKEND_DEVELOPER", "name", "체험 백엔드 개발자",
                    "description", "합성 시연 역할"), null);
        }
        roles = call(journey, "GET", "/api/v1/roles", null, null);
        if (roles.size() != 1 || !roles.get(0).path("roleType").asText().equals("ROLE_BACKEND_DEVELOPER")) {
            throw new IllegalStateException("Journey role is not at template start");
        }
        long roleId = roles.get(0).path("id").asLong();
        JsonNode records = call(journey, "GET", "/api/v1/lifelogs", null, null);
        if (records.path("content").isEmpty()) {
            call(journey, "POST", "/api/v1/players/collections", Map.of(
                    "category", "PROJECT", "title", "체험 서비스 프로젝트",
                    "quantity", 1, "lifeLogSubtype", "PROJECT"), null);
        }
        records = call(journey, "GET", "/api/v1/lifelogs", null, null);
        if (records.path("content").size() != 1 ||
                !records.path("content").get(0).path("subtype").asText().equals("PROJECT")) {
            throw new IllegalStateException("Journey PROJECT record is not at template start");
        }
        long projectLifeLogId = records.path("content").get(0).path("lifeLogId").asLong();

        call(explorer, "POST", "/api/v1/follows", Map.of("targetPlayerId", seller.playerId()), null);
        call(seller, "POST", "/api/v1/follows", Map.of("targetPlayerId", explorer.playerId()), null);
        JsonNode channels = call(explorer, "GET", "/api/v1/chat/channels/friends", null, null);
        if (channels.isEmpty()) {
            call(explorer, "POST", "/api/v1/chat/channels/friend/" + seller.playerId(), Map.of(), null);
        }
        channels = call(explorer, "GET", "/api/v1/chat/channels/friends", null, null);
        if (channels.size() != 1 || channels.get(0).path("peer").path("playerId").asLong() != seller.playerId()) {
            throw new IllegalStateException("Friend channel is not at template start");
        }
        store.saveTargets(runId, new DemoRunStore.Targets(
                listingId, roleId, projectLifeLogId, channels.get(0).path("channelId").asLong()));
    }

    private boolean hasUnbound(JsonNode entries) {
        for (JsonNode entry : entries) if (!entry.path("bound").asBoolean(true)) return true;
        return false;
    }
}
