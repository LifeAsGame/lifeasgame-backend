package online.lifeasgame.quest.api.player;

import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.quest.application.QuestRouteAdvanceService;
import online.lifeasgame.quest.application.QuestRouteQueryService;
import online.lifeasgame.quest.application.QuestRouteSelectService;
import online.lifeasgame.quest.application.result.QuestRouteResult;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestRouteController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("QuestRoute player API")
class QuestRouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestRouteSelectService selectService;

    @MockitoBean
    private QuestRouteAdvanceService advanceService;

    @MockitoBean
    private QuestRouteQueryService queryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("Route catalog를 조회할 때")
    class QueryCatalog {

        @Test
        @DisplayName("definition과 미선택 Step 상태를 반환한다")
        void returnsRouteCatalog() throws Exception {
            when(queryService.routes()).thenReturn(
                    new QuestRouteResult.Routes(List.of(route(null)))
            );

            mockMvc.perform(authenticated(get("/api/v1/quest-routes")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.routes[0].code")
                            .value("ROUTE_RECORD_START"))
                    .andExpect(jsonPath("$.result.routes[0].steps[0].state")
                            .value("LOCKED"));

            verify(queryService).routes();
        }

        @Test
        @DisplayName("고정된 /my 경로는 routeId 상세와 충돌하지 않는다")
        void returnsMySelectedRoutes() throws Exception {
            when(queryService.myRoutes()).thenReturn(
                    new QuestRouteResult.Routes(List.of(route(progress(350L))))
            );

            mockMvc.perform(authenticated(get("/api/v1/quest-routes/my")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.routes[0].playerProgress.status")
                            .value("IN_PROGRESS"));

            verify(queryService).myRoutes();
        }

        @Test
        @DisplayName("내 Route의 Step 상세 경로를 그대로 매핑한다")
        void returnsMyStepDetail() throws Exception {
            QuestRouteResult.Route route = route(progress(350L));
            when(queryService.myStep(250L, 350L)).thenReturn(
                    new QuestRouteResult.StepDetail(
                            route.id(),
                            route.code(),
                            route.playerProgress(),
                            route.steps().getFirst()
                    )
            );

            mockMvc.perform(authenticated(get(
                            "/api/v1/quest-routes/my/{routeId}/steps/{stepId}",
                            250L,
                            350L
                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.routeId").value(250L))
                    .andExpect(jsonPath("$.result.step.id").value(350L));

            verify(queryService).myStep(250L, 350L);
        }
    }

    @Nested
    @DisplayName("Route를 선택할 때")
    class SelectRoute {

        @Test
        @DisplayName("path routeId만 전달하고 선택된 runtime을 반환한다")
        void selectsWithSelfIdentityOwnedByService() throws Exception {
            when(selectService.select(250L)).thenReturn(route(progress(350L)));

            mockMvc.perform(authenticated(post(
                            "/api/v1/quest-routes/{routeId}/select",
                            250L
                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.playerProgress.currentStepId")
                            .value(350L));

            verify(selectService).select(250L);
        }
    }

    @Nested
    @DisplayName("현재 Step을 진행할 때")
    class AdvanceRoute {

        @Test
        @DisplayName("request의 expectedStepId만 command 경계로 전달한다")
        void advancesWithExpectedStepOnly() throws Exception {
            when(advanceService.advance(250L, 350L))
                    .thenReturn(route(progress(351L)));

            mockMvc.perform(authenticated(post(
                            "/api/v1/quest-routes/my/{routeId}/advance",
                            250L
                    )).contentType("application/json")
                            .content("{\"expectedStepId\":350}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.playerProgress.currentStepId")
                            .value(351L));

            verify(advanceService).advance(250L, 350L);
        }

        @Test
        @DisplayName("expectedStepId가 없으면 Application Service를 호출하지 않고 400을 반환한다")
        void rejectsMissingExpectedStep() throws Exception {
            mockMvc.perform(authenticated(post(
                            "/api/v1/quest-routes/my/{routeId}/advance",
                            250L
                    )).contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request
    ) {
        return request.with(authentication(
                        new UsernamePasswordAuthenticationToken(
                                new JwtPrincipal(250L, 250001L),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .header("Authorization", "Bearer test-token");
    }

    private QuestRouteResult.Route route(
            QuestRouteResult.PlayerProgress progress
    ) {
        return new QuestRouteResult.Route(
                250L,
                "ROUTE_RECORD_START",
                1,
                "기록으로 시작하기",
                "기록을 남기는 방향",
                null,
                progress,
                List.of(new QuestRouteResult.Step(
                        350L,
                        "RS_RECORD_01_LEAVE_TRACE",
                        1,
                        "첫 흔적 남기기",
                        null,
                        "QUEST_COMPLETION_SET",
                        1,
                        true,
                        true,
                        false,
                        false,
                        progress == null ? "LOCKED" : "CURRENT",
                        List.of(new QuestRouteResult.QuestLink(
                                450L,
                                "REQUIRED"
                        ))
                ))
        );
    }

    private QuestRouteResult.PlayerProgress progress(Long currentStepId) {
        return new QuestRouteResult.PlayerProgress(
                550L,
                currentStepId,
                "IN_PROGRESS",
                Instant.parse("2026-08-10T01:00:00Z"),
                null
        );
    }
}
