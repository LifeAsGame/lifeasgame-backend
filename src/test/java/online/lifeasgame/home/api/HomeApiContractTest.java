package online.lifeasgame.home.api;

import online.lifeasgame.home.application.HomeQueryService;
import online.lifeasgame.home.application.result.HomeResult;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.quest.application.internal.QuestProgressReadApi;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HomeController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Home player API")
class HomeApiContractTest {

    private static final Instant GENERATED_AT =
            Instant.parse("2026-08-12T00:00:00Z");
    private static final Instant WINDOW_START =
            Instant.parse("2026-07-13T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    private HomeQueryService homeQueryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("Home endpoint를 노출할 때")
    class Endpoint {

        @Test
        @DisplayName("parameter 없는 GET /api/v1/home만 노출한다")
        void exposesExactSelfMapping() throws Exception {
            assertThat(handlerMapping.getHandlerMethods().keySet())
                    .anyMatch(mapping ->
                            mapping.getMethodsCondition().getMethods()
                                    .contains(RequestMethod.GET)
                                    && mapping.getPatternValues()
                                    .contains("/api/v1/home")
                    );
            assertThat(HomeController.class.getDeclaredMethod("home")
                    .getParameterCount()).isZero();
        }

        @Test
        @DisplayName("인증이 없으면 401이다")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/home"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("인증된 Player가 Home을 조회할 때")
    class ReadHome {

        @Test
        @DisplayName("기록이 없으면 null 없는 빈 world summary를 반환한다")
        void returnsEmptySummary() throws Exception {
            given(homeQueryService.home()).willReturn(emptySummary());

            mockMvc.perform(authenticatedGet("/api/v1/home"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.generatedAt")
                            .value("2026-08-12T00:00:00Z"))
                    .andExpect(jsonPath("$.result.recentJournal").isArray())
                    .andExpect(jsonPath("$.result.recentJournal").isEmpty())
                    .andExpect(jsonPath("$.result.journey.currentQuests")
                            .isEmpty())
                    .andExpect(jsonPath("$.result.journey.selectedRoutes")
                            .isEmpty())
                    .andExpect(jsonPath(
                            "$.result.roleActivity30d.totalRecords"
                    ).value(0))
                    .andExpect(jsonPath(
                            "$.result.roleActivity30d.roles"
                    ).isEmpty());
        }

        @Test
        @DisplayName("playerId query를 받지 않고 composed section을 반환한다")
        void returnsPopulatedSelfSummary() throws Exception {
            given(homeQueryService.home()).willReturn(populatedSummary());

            mockMvc.perform(authenticatedGet(
                            "/api/v1/home?playerId=999&userId=888"
                    ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(
                            "$.result.recentJournal[0].lifeLogId"
                    ).value(101))
                    .andExpect(jsonPath(
                            "$.result.recentJournal[0].sourceType"
                    ).value("COLLECTION"))
                    .andExpect(jsonPath(
                            "$.result.recentJournal[0].entryMode"
                    ).value("QUICK"))
                    .andExpect(jsonPath(
                            "$.result.recentJournal[0].preview.title"
                    ).value("기록"))
                    .andExpect(jsonPath(
                            "$.result.recentJournal[0].sourceId"
                    ).doesNotExist())
                    .andExpect(jsonPath(
                            "$.result.journey.currentQuests[0].status"
                    ).value("GOAL_REACHED"))
                    .andExpect(jsonPath(
                            "$.result.journey.selectedRoutes[0].routeCode"
                    ).value("ROUTE_SELF"))
                    .andExpect(jsonPath(
                            "$.result.roleActivity30d.roles[0].roleName"
                    ).value("개발자"))
                    .andExpect(jsonPath(
                            "$.result.roleActivity30d.roles[0].share"
                    ).value(1.0));
            verify(homeQueryService).home();
        }
    }

    private HomeResult.Summary emptySummary() {
        return new HomeResult.Summary(
                GENERATED_AT,
                List.of(),
                new HomeResult.Journey(List.of(), List.of()),
                new HomeResult.RoleActivity(
                        WINDOW_START,
                        GENERATED_AT,
                        0,
                        0,
                        0,
                        List.of()
                )
        );
    }

    private HomeResult.Summary populatedSummary() {
        return new HomeResult.Summary(
                GENERATED_AT,
                List.of(new LifeLogJournalResult.Entry(
                        101L,
                        "COLLECTION",
                        201L,
                        null,
                        "QUICK",
                        null,
                        null,
                        31L,
                        null,
                        GENERATED_AT.minusSeconds(60),
                        new LifeLogJournalResult.CollectionPreview(
                                "BOOK",
                                "기록",
                                1
                        )
                )),
                new HomeResult.Journey(
                        List.of(new QuestProgressReadApi.CurrentQuest(
                                301L,
                                "Q_SELF",
                                "현재 퀘스트",
                                "GOAL_REACHED",
                                3,
                                3,
                                GENERATED_AT.minusSeconds(300),
                                GENERATED_AT.minusSeconds(60)
                        )),
                        List.of(new QuestProgressReadApi.SelectedRoute(
                                401L,
                                "ROUTE_SELF",
                                "나의 경로",
                                "IN_PROGRESS",
                                501L,
                                GENERATED_AT.minusSeconds(600),
                                null
                        ))
                ),
                new HomeResult.RoleActivity(
                        WINDOW_START,
                        GENERATED_AT,
                        2,
                        2,
                        0,
                        List.of(new HomeResult.RoleBucket(
                                31L,
                                "개발자",
                                2,
                                1.0
                        ))
                )
        );
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    authenticatedGet(String path) {
        return get(path)
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(
                                new JwtPrincipal(258L, 25801L),
                                null,
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_USER"
                                ))
                        )
                ))
                .header("Authorization", "Bearer test-token");
    }
}
