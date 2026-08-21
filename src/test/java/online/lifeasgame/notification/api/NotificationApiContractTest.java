package online.lifeasgame.notification.api;

import java.time.Instant;
import java.util.List;
import online.lifeasgame.notification.application.NotificationQueryService;
import online.lifeasgame.notification.application.NotificationReadMarker;
import online.lifeasgame.notification.application.result.NotificationResult;
import online.lifeasgame.notification.domain.NotificationType;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Current Player Notification API contract")
class NotificationApiContractTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-21T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationQueryService queryService;

    @MockitoBean
    private NotificationReadMarker readMarker;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("GET inbox는 기본 size와 공개 필드만 ApiResponse로 반환한다")
    void returnsInboxEnvelopeWithoutPrivateFields() throws Exception {
        given(queryService.inbox(null, 20)).willReturn(
                new NotificationResult.Page(
                        List.of(new NotificationResult.Info(
                                292L,
                                NotificationType.QUEST_COMPLETED,
                                "퀘스트 완료",
                                "첫 퀘스트를 완료했습니다.",
                                OCCURRED_AT,
                                false
                        )),
                        true,
                        292L
                )
        );

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(playerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result.*", hasSize(3)))
                .andExpect(jsonPath("$.result.notifications", hasSize(1)))
                .andExpect(jsonPath("$.result.notifications[0].*", hasSize(6)))
                .andExpect(jsonPath("$.result.notifications[0].id").value(292))
                .andExpect(jsonPath("$.result.notifications[0].type")
                        .value("QUEST_COMPLETED"))
                .andExpect(jsonPath("$.result.notifications[0].title")
                        .value("퀘스트 완료"))
                .andExpect(jsonPath("$.result.notifications[0].body")
                        .value("첫 퀘스트를 완료했습니다."))
                .andExpect(jsonPath("$.result.notifications[0].occurredAt")
                        .value("2026-08-21T10:00:00Z"))
                .andExpect(jsonPath("$.result.notifications[0].read").value(false))
                .andExpect(jsonPath("$.result.notifications[0].playerId").doesNotExist())
                .andExpect(jsonPath("$.result.notifications[0].userId").doesNotExist())
                .andExpect(jsonPath("$.result.notifications[0].sourceEventId").doesNotExist())
                .andExpect(jsonPath("$.result.notifications[0].readAt").doesNotExist())
                .andExpect(jsonPath("$.result.hasMore").value(true))
                .andExpect(jsonPath("$.result.nextCursor").value(292));

        verify(queryService).inbox(null, 20);
    }

    @Test
    @DisplayName("GET unread-count는 Current Player count를 ApiResponse로 반환한다")
    void returnsUnreadCount() throws Exception {
        given(queryService.unreadCount()).willReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(playerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.*", hasSize(1)))
                .andExpect(jsonPath("$.result.unreadCount").value(3));
    }

    @Test
    @DisplayName("POST notification read는 identity body 없이 idempotent success를 반환한다")
    void marksOneRead() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/{id}/read", 292L)
                        .with(authentication(playerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(readMarker).markOne(292L);
    }

    @Test
    @DisplayName("POST read-all은 Current Player markedCount를 반환한다")
    void marksAllRead() throws Exception {
        given(readMarker.markAll()).willReturn(4);

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .with(authentication(playerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.*", hasSize(1)))
                .andExpect(jsonPath("$.result.markedCount").value(4));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    @DisplayName("inbox size가 1~100 밖이면 400을 반환한다")
    void rejectsInvalidSize(int size) throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .param("size", Integer.toString(size))
                        .with(authentication(playerAuthentication())))
                .andExpect(status().isBadRequest());
    }

    private UsernamePasswordAuthenticationToken playerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(29201L, 292001L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
