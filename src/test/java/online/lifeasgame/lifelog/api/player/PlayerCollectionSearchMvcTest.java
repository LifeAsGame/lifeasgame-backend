package online.lifeasgame.lifelog.api.player;

import online.lifeasgame.lifelog.api.admin.AdminCollectionController;
import online.lifeasgame.lifelog.application.CollectionLogQueryService;
import online.lifeasgame.lifelog.application.CollectionLogService;
import online.lifeasgame.lifelog.application.query.CollectionQuery;
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

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;

@WebMvcTest({PlayerCollectionController.class, AdminCollectionController.class})
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Collection 검색 MVC 검증")
class PlayerCollectionSearchMvcTest {

    private static final String PLAYER_SEARCH = "/api/v1/players/collections/search";
    private static final String ADMIN_SEARCH = "/admin/v1/players/25601/collections/search";

    @Autowired MockMvc mockMvc;

    @MockitoBean CollectionLogService collectionLogService;
    @MockitoBean CollectionLogQueryService collectionLogQueryService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean AppErrorProperties appErrorProperties;
    @MockitoBean ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("인증된 검색은 기본 page=0, size=20으로 raw list를 반환한다")
    void searchDefaults() throws Exception {
        mockMvc.perform(authenticatedGet(PLAYER_SEARCH, "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(collectionLogQueryService).search(new CollectionQuery.Search(null, null, 0, 20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=0&size=1", "page=1&size=100", "page=0&size=20"})
    @DisplayName("검색 경계값과 정상 page/size는 200이다")
    void searchValid(String query) throws Exception {
        mockMvc.perform(authenticatedGet(PLAYER_SEARCH + "?" + query, "ROLE_USER"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101"})
    @DisplayName("검색 범위를 벗어나면 400이다")
    void searchInvalid(String query) throws Exception {
        mockMvc.perform(authenticatedGet(PLAYER_SEARCH + "?" + query, "ROLE_USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQ-VALIDATION"));
    }

    @Test
    @DisplayName("인증된 최근 조회는 200이다")
    void recent() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/players/collections/recent", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(collectionLogQueryService).recent(20);
    }

    @Test
    @DisplayName("recent limit 범위도 유지한다")
    void recentInvalid() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/players/collections/recent?limit=0", "ROLE_USER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("관리자 검색은 200이다")
    void adminSearch() throws Exception {
        mockMvc.perform(authenticatedGet(ADMIN_SEARCH + "?page=0&size=20", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("관리자 recent도 기존 envelope를 반환한다")
    void adminRecent() throws Exception {
        mockMvc.perform(authenticatedGet("/admin/v1/players/25601/collections/recent", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101"})
    @DisplayName("관리자 검색도 같은 범위를 검증한다")
    void adminSearchInvalid(String query) throws Exception {
        mockMvc.perform(authenticatedGet(ADMIN_SEARCH + "?" + query, "ROLE_ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQ-VALIDATION"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(
            String path, String role
    ) {
        return get(path)
                .with(authentication(new UsernamePasswordAuthenticationToken(
                        new JwtPrincipal(256L, 25601L), null,
                        List.of(new SimpleGrantedAuthority(role)))))
                .header("Authorization", "Bearer test-token");
    }
}
