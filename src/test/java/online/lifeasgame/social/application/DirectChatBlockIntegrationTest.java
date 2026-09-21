package online.lifeasgame.social.application;

import jakarta.persistence.EntityManager;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtCurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.social.application.command.ChatCommand;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(DirectChatBlockIntegrationTest.IdentityConfig.class)
@RecordApplicationEvents
@DisplayName("Direct Chat 차단 HTTP·MySQL 및 block/send 순서 계약")
class DirectChatBlockIntegrationTest {

    private static final long A = 34801L;
    private static final long B = 34802L;
    private static final ChatCommand.SendMessage MESSAGE = new ChatCommand.SendMessage("hello");

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_direct_chat_block")
            .withUsername("lifeasgame").withPassword("lifeasgame");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private ChatService chatService;
    @Autowired private FollowService followService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MockMvc mvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private ApplicationEvents events;
    @MockitoBean private UserAuthApi userAuthApi;
    @MockitoBean private ChatRealtimeGateway gateway;
    @MockitoSpyBean private DomainEventPublisher publisher;

    private long channelId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM chat_messages");
        jdbc.update("DELETE FROM channel_participants");
        jdbc.update("DELETE FROM chat_channels");
        jdbc.update("DELETE FROM follows");
        for (long actor : List.of(A, B)) {
            jdbc.update("""
                    INSERT INTO follows (player_id, target_player_id, state, blocked, muted, created_at, updated_at)
                    VALUES (?, ?, 'FOLLOWING', false, false, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                    """, actor, peer(actor));
        }
        jdbc.update("""
                INSERT INTO chat_channels (type, name, read_only, version, created_at, updated_at)
                VALUES ('FRIEND', 'History', false, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """);
        channelId = jdbc.queryForObject("SELECT id FROM chat_channels", Long.class);
        for (long actor : List.of(A, B)) {
            jdbc.update("""
                    INSERT INTO channel_participants (channel_id, user_id, role, version, created_at, updated_at)
                    VALUES (?, ?, 'MEMBER', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                    """, channelId, actor);
        }
        jdbc.update("""
                INSERT INTO chat_messages (channel_id, sender_id, content, edited, version, created_at, updated_at)
                VALUES (?, ?, 'history', false, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, channelId, A);
        clearInvocations(gateway, publisher);
        events.clear();
    }

    @ParameterizedTest
    @CsvSource({"34801,34801", "34801,34802", "34802,34801", "34802,34802"})
    @DisplayName("어느 방향의 차단이든 양쪽 actor의 HTTP open·send와 다른 send 진입점을 부작용 없이 거부한다")
    void rejectsBothActors(long blocker, long actor) throws Exception {
        followService.block(blocker, followId(blocker));
        var before = snapshot();

        assertBlockedHttp(open(actor), actor);
        assertBlockedHttp(send(), actor);
        assertBlocked(() -> chatService.sendMessage(actor, channelId, MESSAGE));

        assertUnchanged(before);
        mvc.perform(auth(get(messagesPath()), actor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.messages[0].content").value("history"));
        mvc.perform(auth(get("/api/v1/chat/channels"), actor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.friend[0].id").value(channelId));
        assertUnchanged(before);
    }

    @ParameterizedTest
    @CsvSource({"34801,34801", "34801,34802", "34802,34801", "34802,34802"})
    @DisplayName("STOPPED Follow의 차단도 신규 채널·참가자 생성 전에 거부한다")
    void rejectsNewOpenWithStoppedBlock(long blocker, long actor) throws Exception {
        followService.block(blocker, followId(blocker));
        followService.unfollow(blocker, followId(blocker));
        var existing = snapshot();
        assertBlockedHttp(send(), actor);
        assertUnchanged(existing);
        jdbc.update("DELETE FROM chat_messages");
        jdbc.update("DELETE FROM channel_participants");
        jdbc.update("DELETE FROM chat_channels");
        var before = snapshot();

        assertBlockedHttp(open(actor), actor);

        assertUnchanged(before);
    }

    @Test
    @DisplayName("차단이 없으면 open과 양쪽 전송이 동작하고 unfollow만으로 기존 send를 새로 금지하지 않는다")
    void preservesUnblockedPolicy() throws Exception {
        mvc.perform(auth(open(A), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(channelId));
        mvc.perform(auth(send(), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.senderId").value(A));
        followService.unfollow(A, followId(A));
        assertThat(chatService.sendMessage(B, channelId, MESSAGE).senderId()).isEqualTo(B);
        mvc.perform(auth(open(A), A)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SOC-404-NOT-FRIEND"));
        assertThat(messageCount()).isEqualTo(3);
        verify(gateway, times(2)).publish(any());
    }

    @Test
    @DisplayName("신규 open은 두 참가자를 만들고 unblock 후 기존 채널 전송을 다시 허용한다")
    void createsUnblockedChannelAndRestoresAfterUnblock() throws Exception {
        jdbc.update("DELETE FROM chat_messages");
        jdbc.update("DELETE FROM channel_participants");
        jdbc.update("DELETE FROM chat_channels");
        mvc.perform(auth(open(A), A)).andExpect(status().isOk());
        channelId = jdbc.queryForObject("SELECT id FROM chat_channels", Long.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_participants", Long.class)).isEqualTo(2);
        followService.block(B, followId(B));
        assertBlocked(() -> chatService.sendMessage(A, channelId, MESSAGE));
        followService.unblock(B, followId(B));
        assertThat(chatService.sendMessage(A, channelId, MESSAGE).senderId()).isEqualTo(A);
        verify(gateway).publish(any());
    }

    @Test
    @DisplayName("비인증·비멤버·없는 채널 보호가 차단 검사보다 먼저 적용된다")
    void preservesAccessProtection() throws Exception {
        followService.block(A, followId(A));
        var before = snapshot();
        mvc.perform(open(A)).andExpect(status().isUnauthorized());
        mvc.perform(send()).andExpect(status().isUnauthorized());
        mvc.perform(get(messagesPath())).andExpect(status().isUnauthorized());
        mvc.perform(auth(send(), B + 1)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOC-403-CHANNEL-FORBIDDEN"));
        mvc.perform(auth(get(messagesPath()), B + 1)).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/v1/chat/channels/0/messages")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}"), A))
                .andExpect(status().isNotFound());
        assertUnchanged(before);
    }

    @Test
    @DisplayName("관리자 HTTP의 명시적 sender 진입점도 차단되며 이력 조회와 페이지 검증은 유지된다")
    void protectsOperatorEntryPoint() throws Exception {
        followService.block(B, followId(B));
        var before = snapshot();
        given(userAuthApi.resolveAuthorization(A)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true)));
        String token = "Bearer " + jwtProvider.createAccessToken(A, A);
        String path = "/admin/v1/operators/" + A + "/chat/channels/" + channelId + "/messages";
        mvc.perform(post(path).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOC-403-CHAT-DIRECT-BLOCKED"));
        mvc.perform(get(path).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.messages[0].content").value("history"));
        mvc.perform(get(path).header("Authorization", token).param("size", "101"))
                .andExpect(status().isBadRequest());
        assertUnchanged(before);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    @DisplayName("메시지 이력의 페이지 크기 범위 위반은 500이 아닌 400으로 거부한다")
    void validatesHistoryPageSize(int size) throws Exception {
        mvc.perform(auth(get(messagesPath()).param("size", String.valueOf(size)), A))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    @DisplayName("Direct peer가 정확히 한 명이 아니면 전송을 fail-closed로 거부한다")
    void rejectsMalformedParticipants(int peers) {
        jdbc.update("DELETE FROM channel_participants WHERE user_id = ?", B);
        for (int i = 0; i < peers; i++) {
            jdbc.update("""
                    INSERT INTO channel_participants (channel_id, user_id, role, version, created_at, updated_at)
                    VALUES (?, ?, 'MEMBER', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                    """, channelId, B + i);
        }
        var before = snapshot();
        assertError(() -> chatService.sendMessage(A, channelId, MESSAGE), SocialError.CHAT_FRIEND_PARTICIPANT_INVALID);
        assertUnchanged(before);
    }

    @Test
    @DisplayName("read-only 보호와 Direct 이외 채널 전송 정책을 유지한다")
    void preservesOtherChannelPolicy() {
        followService.block(A, followId(A));
        jdbc.update("UPDATE chat_channels SET read_only = true WHERE id = ?", channelId);
        var before = snapshot();
        assertError(() -> chatService.sendMessage(A, channelId, MESSAGE), SocialError.CHAT_CHANNEL_READ_ONLY);
        assertUnchanged(before);
        jdbc.update("UPDATE chat_channels SET type = 'GLOBAL', read_only = false WHERE id = ?", channelId);
        assertThat(chatService.sendMessage(A, channelId, MESSAGE).senderId()).isEqualTo(A);
        verify(gateway).publish(any());
    }

    @ParameterizedTest
    @CsvSource({"34801,34801", "34801,34802", "34802,34801", "34802,34802"})
    @DisplayName("block이 먼저 행을 잠그면 send는 실제 DB 잠금에서 기다린 뒤 커밋된 차단으로 거부된다")
    void blockWins(long blocker, long sender) throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            AtomicReference<Future<?>> pending = new AtomicReference<>();
            transaction().executeWithoutResult(status -> {
                followService.block(blocker, followId(blocker));
                entityManager.flush();
                pending.set(executor.submit(() -> assertBlocked(() -> chatService.sendMessage(sender, channelId, MESSAGE))));
                awaitFollowLockWait();
                assertThat(pending.get()).isNotDone();
            });
            pending.get().get(10, TimeUnit.SECONDS);
        }
        assertThat(messageCount()).isEqualTo(1);
        verifyNoInteractions(gateway, publisher);
        assertBlocked(() -> chatService.sendMessage(A, channelId, MESSAGE));
        assertBlocked(() -> chatService.sendMessage(B, channelId, MESSAGE));
    }

    @ParameterizedTest
    @CsvSource({"34801,34801", "34801,34802", "34802,34801", "34802,34802"})
    @DisplayName("send가 먼저 잠그면 block 커밋은 전송 커밋을 기다리고 이후 전송은 거부된다")
    void sendWins(long blocker, long sender) throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            AtomicReference<Future<?>> pending = new AtomicReference<>();
            transaction().executeWithoutResult(status -> {
                chatService.sendMessage(sender, channelId, MESSAGE);
                pending.set(executor.submit(() -> followService.block(blocker, followId(blocker))));
                awaitFollowLockWait();
                assertThat(pending.get()).isNotDone();
            });
            pending.get().get(10, TimeUnit.SECONDS);
        }
        assertThat(messageCount()).isEqualTo(2);
        verify(gateway).publish(any());
        assertBlocked(() -> chatService.sendMessage(A, channelId, MESSAGE));
        assertBlocked(() -> chatService.sendMessage(B, channelId, MESSAGE));
        assertThat(messageCount()).isEqualTo(2);
        verify(gateway).publish(any());
    }

    @Test
    @DisplayName("기존 repeatable-read snapshot과 managed Follow가 오래됐어도 커밋된 차단을 읽는다")
    void rejectsDespiteStaleSnapshot() {
        assertBlocked(() -> transaction().executeWithoutResult(status -> {
            Follow stale = entityManager.find(Follow.class, followId(A));
            assertThat(stale.isBlocked()).isFalse();
            runSeparately(() -> followService.block(A, followId(A)));
            assertThat(stale.isBlocked()).isFalse();
            chatService.sendMessage(B, channelId, MESSAGE);
        }));
        assertThat(messageCount()).isEqualTo(1);
        verifyNoInteractions(gateway, publisher);
    }

    @Test
    @DisplayName("오래된 Follow의 unfollow 갱신이 이미 커밋된 blocked 값을 덮어쓰지 않는다")
    void staleUnfollowPreservesBlock() {
        transaction().executeWithoutResult(status -> {
            entityManager.find(Follow.class, followId(A));
            runSeparately(() -> followService.block(A, followId(A)));
            followService.unfollow(A, followId(A));
        });
        assertThat(jdbc.queryForObject("SELECT blocked FROM follows WHERE player_id = ?", Boolean.class, A)).isTrue();
        assertBlocked(() -> chatService.sendMessage(B, channelId, MESSAGE));
        verifyNoInteractions(gateway, publisher);
    }

    private void awaitFollowLockWait() {
        // Observe an actual MySQL lock wait instead of using scheduler timing as evidence.
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
             var statement = connection.createStatement()) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                try (var rows = statement.executeQuery("""
                        SELECT COUNT(*) FROM performance_schema.data_lock_waits w
                        JOIN performance_schema.data_locks l ON l.ENGINE_LOCK_ID = w.REQUESTING_ENGINE_LOCK_ID
                        WHERE l.OBJECT_SCHEMA = 'lifeasgame_direct_chat_block' AND l.OBJECT_NAME = 'follows'
                        """)) {
                    rows.next();
                    if (rows.getLong(1) > 0) return;
                }
                Thread.sleep(20);
            }
            throw new AssertionError("Expected a Follow row lock wait");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void runSeparately(Runnable action) {
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(action).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private TransactionTemplate transaction() { return new TransactionTemplate(transactionManager); }
    private long peer(long actor) { return actor == A ? B : A; }
    private long followId(long actor) {
        return jdbc.queryForObject("SELECT follow_id FROM follows WHERE player_id = ?", Long.class, actor);
    }
    private long messageCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages", Long.class); }
    private String messagesPath() { return "/api/v1/chat/channels/" + channelId + "/messages"; }
    private MockHttpServletRequestBuilder send() {
        return post(messagesPath()).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}");
    }
    private MockHttpServletRequestBuilder open(long actor) {
        return post("/api/v1/chat/channels/friend/" + peer(actor)).contentType(MediaType.APPLICATION_JSON).content("{}");
    }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, long actor) {
        given(userAuthApi.resolveAuthorization(actor)).willReturn(Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        return request.header("Authorization", "Bearer " + jwtProvider.createAccessToken(actor, actor));
    }
    private void assertBlockedHttp(MockHttpServletRequestBuilder request, long actor) throws Exception {
        mvc.perform(auth(request, actor)).andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("SOC-403-CHAT-DIRECT-BLOCKED"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }
    private void assertBlocked(Runnable action) { assertError(action, SocialError.CHAT_DIRECT_BLOCKED); }
    private void assertError(Runnable action, SocialError error) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(error));
    }
    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : List.of("chat_channels", "channel_participants", "chat_messages", "follows", "outbox_events")) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY " + (table.equals("follows") ? "follow_id" : "id")));
        }
        return result;
    }
    private void assertUnchanged(Map<String, List<Map<String, Object>>> before) {
        assertThat(snapshot()).isEqualTo(before);
        verifyNoInteractions(gateway, publisher);
        assertThat(events.stream(DomainEvent.class)).isEmpty();
    }

    @TestConfiguration
    static class IdentityConfig {
        @Bean
        @Primary
        CurrentPlayerAccessor directChatTestIdentity() { return new JwtCurrentPlayerAccessor(); }
    }
}
