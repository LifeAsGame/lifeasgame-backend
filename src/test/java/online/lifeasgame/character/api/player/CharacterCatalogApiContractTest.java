package online.lifeasgame.character.api.player;

import java.time.LocalDate;
import java.util.List;
import online.lifeasgame.character.application.CharacterCatalogService;
import online.lifeasgame.character.application.PersonalCategoryService;
import online.lifeasgame.character.application.PrivateHobbyService;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PrivateHobbyController.class, CharacterCatalogController.class, PersonalCategoryController.class})
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Character catalog v2 HTTP boundary")
class CharacterCatalogApiContractTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PrivateHobbyService privateHobbies;
    @MockitoBean CharacterCatalogService catalog;
    @MockitoBean PersonalCategoryService categories;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean AppErrorProperties appErrorProperties;
    @MockitoBean ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("독립 취미 생성은 owned ID와 null catalog ID를 반환하고 날짜 오류는 400으로 차단한다")
    void privateCreateAndDateValidation() throws Exception {
        given(privateHobbies.create(eq("나의 취미"), isNull(), isNull(), isNull(),
                eq(LocalDate.of(2025, 2, 28)), isNull()))
                .willReturn(new PrivateHobbyService.Info(901L, null, "PRIVATE", "나의 취미",
                        null, 0, "ACTIVE", LocalDate.of(2025, 2, 28), null));
        mockMvc.perform(post("/api/v1/players/hobbies/private")
                        .with(authentication(player())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"나의 취미\",\"startedOn\":\"2025-02-28\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/players/hobbies/private/901"))
                .andExpect(jsonPath("$.result.ownedItemId").value(901))
                .andExpect(jsonPath("$.result.catalogItemId").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(post("/api/v1/players/hobbies/private")
                        .with(authentication(player())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"나의 취미\",\"startedOn\":\"2025-02-29\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("개인 취미와 카탈로그 경로는 인증을 요구한다")
    void authenticatedOnly() throws Exception {
        mockMvc.perform(get("/api/v1/players/hobbies/private")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/catalog/CERTIFICATION/items")).andExpect(status().isUnauthorized());
        verifyNoInteractions(privateHobbies, catalog);
    }

    private UsernamePasswordAuthenticationToken player() {
        return new UsernamePasswordAuthenticationToken(new JwtPrincipal(101L, 101L), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
