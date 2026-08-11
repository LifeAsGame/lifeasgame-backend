package online.lifeasgame.lifelog.api.player;

import online.lifeasgame.lifelog.application.LifeLogJournalQueryService;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerLifeLogJournalController.class)
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class
})
@DisplayName("LifeLog Journal player API")
class PlayerLifeLogJournalApiContractTest {

    private static final Instant RECORDED_AT =
            Instant.parse("2026-08-11T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    private LifeLogJournalQueryService queryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("self Journal endpoint를 노출할 때")
    class Endpoints {

        @Test
        @DisplayName("list와 canonical detail GET mapping만 추가한다")
        void exposesExactReadMappings() {
            assertMapping(RequestMethod.GET, "/api/v1/lifelogs");
            assertMapping(
                    RequestMethod.GET,
                    "/api/v1/lifelogs/{lifeLogId}"
            );
        }

        @Test
        @DisplayName("인증이 없으면 list와 detail 모두 401이다")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/lifelogs"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/v1/lifelogs/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Journal 목록을 호출할 때")
    class ListJournal {

        @Test
        @DisplayName("self filter와 page만 위임하고 canonical metadata와 preview를 반환한다")
        void returnsCanonicalPage() throws Exception {
            given(queryService.list(31L, "MEMORY", 1, 5))
                    .willReturn(new LifeLogJournalResult.Page(
                            List.of(new LifeLogJournalResult.Entry(
                                    101L,
                                    "COLLECTION",
                                    201L,
                                    "MEMORY",
                                    "QUICK",
                                    null,
                                    null,
                                    31L,
                                    41L,
                                    RECORDED_AT,
                                    new LifeLogJournalResult.CollectionPreview(
                                            "BOOK",
                                            "기록",
                                            2
                                    )
                            )),
                            1,
                            5,
                            6,
                            2
                    ));

            mockMvc.perform(authenticatedGet(
                            "/api/v1/lifelogs"
                                    + "?primaryRoleId=31"
                                    + "&subtype=MEMORY&page=1&size=5"
                                    + "&playerId=999"
                    ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.page").value(1))
                    .andExpect(jsonPath("$.result.totalElements").value(6))
                    .andExpect(jsonPath("$.result.content[0].lifeLogId")
                            .value(101))
                    .andExpect(jsonPath("$.result.content[0].sourceType")
                            .value("COLLECTION"))
                    .andExpect(jsonPath("$.result.content[0].primaryRoleId")
                            .value(31))
                    .andExpect(jsonPath("$.result.content[0].roleEventId")
                            .value(41))
                    .andExpect(jsonPath("$.result.content[0].recordedAt")
                            .value("2026-08-11T12:00:00Z"))
                    .andExpect(jsonPath("$.result.content[0].preview.title")
                            .value("기록"))
                    .andExpect(jsonPath("$.result.content[0].playerId")
                            .doesNotExist());
            verify(queryService).list(31L, "MEMORY", 1, 5);
        }
    }

    @Nested
    @DisplayName("Journal 상세를 호출할 때")
    class ReadDetail {

        @Test
        @DisplayName("canonical identity와 Media source detail을 반환한다")
        void returnsOwnedSourceDetail() throws Exception {
            given(queryService.detail(101L)).willReturn(
                    new LifeLogJournalResult.Detail(
                            101L,
                            "MEDIA",
                            301L,
                            null,
                            "FULL",
                            null,
                            null,
                            null,
                            null,
                            RECORDED_AT,
                            new LifeLogJournalResult.MediaSource(
                                    "MOVIE",
                                    "영화",
                                    null,
                                    2,
                                    10,
                                    "WATCHING",
                                    4.5,
                                    Set.of("journal"),
                                    0,
                                    LocalDate.of(2026, 8, 1),
                                    null,
                                    RECORDED_AT,
                                    RECORDED_AT
                            )
                    )
            );

            mockMvc.perform(authenticatedGet("/api/v1/lifelogs/101"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.lifeLogId").value(101))
                    .andExpect(jsonPath("$.result.sourceId").value(301))
                    .andExpect(jsonPath("$.result.subtype").doesNotExist())
                    .andExpect(jsonPath("$.result.source.title")
                            .value("영화"))
                    .andExpect(jsonPath("$.result.source.currentEpisode")
                            .value(2))
                    .andExpect(jsonPath("$.result.source.playerId")
                            .doesNotExist());
            verify(queryService).detail(101L);
        }
    }

    private MockHttpServletRequestBuilder authenticatedGet(String path) {
        return get(path)
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(
                                new JwtPrincipal(256L, 25601L),
                                null,
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_USER"
                                ))
                        )
                ))
                .header("Authorization", "Bearer test-token");
    }

    private void assertMapping(RequestMethod method, String path) {
        assertThat(handlerMapping.getHandlerMethods().keySet())
                .anyMatch(mapping ->
                        mapping.getMethodsCondition().getMethods()
                                .contains(method)
                                && mapping.getPatternValues().contains(path)
                );
    }
}
