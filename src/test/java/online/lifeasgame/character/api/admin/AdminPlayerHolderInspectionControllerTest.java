package online.lifeasgame.character.api.admin;

import online.lifeasgame.character.application.PlayerAchievementService;
import online.lifeasgame.character.application.PlayerCertificationService;
import online.lifeasgame.character.application.PlayerHolderQueryService;
import online.lifeasgame.character.application.PlayerHobbyService;
import online.lifeasgame.character.application.PlayerTitleService;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.platform.security.jwt.JwtProperties;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AdminPlayerAchievementController.class,
        AdminPlayerCertificationController.class,
        AdminPlayerHobbyController.class,
        AdminPlayerTitleController.class
})
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminPlayerHolderInspectionControllerTest.JwtTestConfig.class
})
@DisplayName("Admin Player holder inspection contract")
class AdminPlayerHolderInspectionControllerTest {

    private static final long PLAYER_ID = 314L;
    private static final long USER_ID = 31401L;
    private static final long ADMIN_ID = 31402L;
    private static final String SECRET =
            "admin-holder-read-test-secret-at-least-32-characters";
    private static final Instant ACQUIRED_AT =
            Instant.parse("2026-08-26T03:14:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private PlayerHolderQueryService holderQueryService;

    @MockitoBean
    private PlayerAchievementService achievementService;

    @MockitoBean
    private PlayerCertificationService certificationService;

    @MockitoBean
    private PlayerHobbyService hobbyService;

    @MockitoBean
    private PlayerTitleService titleService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @BeforeEach
    void setUp() {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );
    }

    @Test
    @DisplayName("Achievement holder는 definition body 없이 bounded summary를 반환한다")
    void listsAchievements() throws Exception {
        given(holderQueryService.getAchievementInfos(PLAYER_ID)).willReturn(
                List.of(new PlayerAchievementResult.Info(
                        1001L,
                        "ACH-314",
                        "First Step",
                        "CAREER",
                        "private definition body",
                        ACQUIRED_AT
                ))
        );

        mockMvc.perform(adminGet("/admin/v1/players/314/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.playerId").value(PLAYER_ID))
                .andExpect(jsonPath("$.result.infos[0].achievementId").value(1001))
                .andExpect(jsonPath("$.result.infos[0].code").value("ACH-314"))
                .andExpect(jsonPath("$.result.infos[0].name").value("First Step"))
                .andExpect(jsonPath("$.result.infos[0].category").value("CAREER"))
                .andExpect(jsonPath("$.result.infos[0].acquiredAt").value("2026-08-26T03:14:00Z"))
                .andExpect(jsonPath("$.result.infos[0].descMd").doesNotExist());

        verify(holderQueryService).getAchievementInfos(PLAYER_ID);
    }

    @Test
    @DisplayName("Certification holder는 issuer category와 holder dates를 반환한다")
    void listsCertifications() throws Exception {
        given(holderQueryService.getCertificationInfos(PLAYER_ID)).willReturn(
                List.of(new PlayerCertificationResult.Info(
                        2001L,
                        "Java Professional",
                        "Oracle",
                        "PROFESSIONAL",
                        LocalDate.parse("2026-01-02"),
                        LocalDate.parse("2028-01-02"),
                        ACQUIRED_AT
                ))
        );

        mockMvc.perform(adminGet("/admin/v1/players/314/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.playerId").value(PLAYER_ID))
                .andExpect(jsonPath("$.result.infos[0].certificationId").value(2001))
                .andExpect(jsonPath("$.result.infos[0].name").value("Java Professional"))
                .andExpect(jsonPath("$.result.infos[0].issuer").value("Oracle"))
                .andExpect(jsonPath("$.result.infos[0].category").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.result.infos[0].acquiredDate").value("2026-01-02"))
                .andExpect(jsonPath("$.result.infos[0].expiresDate").value("2028-01-02"))
                .andExpect(jsonPath("$.result.infos[0].grantedAt").value("2026-08-26T03:14:00Z"));

        verify(holderQueryService).getCertificationInfos(PLAYER_ID);
    }

    @Test
    @DisplayName("Hobby holder는 user-authored customName과 detail을 제외한다")
    void listsHobbiesWithoutFreeformFields() throws Exception {
        given(holderQueryService.getHobbyInfos(PLAYER_ID)).willReturn(
                List.of(new PlayerHobbyResult.Info(
                        3001L,
                        "Running",
                        "SPORTS",
                        "My private custom name",
                        "My private notes",
                        73,
                        "ACTIVE",
                        LocalDate.parse("2025-04-03"),
                        1200L
                ))
        );

        mockMvc.perform(adminGet("/admin/v1/players/314/hobbies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.playerId").value(PLAYER_ID))
                .andExpect(jsonPath("$.result.infos[0].hobbyId").value(3001))
                .andExpect(jsonPath("$.result.infos[0].name").value("Running"))
                .andExpect(jsonPath("$.result.infos[0].category").value("SPORTS"))
                .andExpect(jsonPath("$.result.infos[0].proficiency").value(73))
                .andExpect(jsonPath("$.result.infos[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.infos[0].startedOn").value("2025-04-03"))
                .andExpect(jsonPath("$.result.infos[0].xp").value(1200))
                .andExpect(jsonPath("$.result.infos[0].customName").doesNotExist())
                .andExpect(jsonPath("$.result.infos[0].detail").doesNotExist());

        verify(holderQueryService).getHobbyInfos(PLAYER_ID);
    }

    @Test
    @DisplayName("Title holder는 definition body와 representative state 없이 bounded summary를 반환한다")
    void listsTitles() throws Exception {
        given(holderQueryService.getTitleInfos(PLAYER_ID)).willReturn(
                List.of(new PlayerTitleResult.Info(
                        4001L,
                        "TITLE-314",
                        "Trailblazer",
                        "CAREER",
                        "private definition body",
                        ACQUIRED_AT
                ))
        );

        mockMvc.perform(adminGet("/admin/v1/players/314/titles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.playerId").value(PLAYER_ID))
                .andExpect(jsonPath("$.result.infos[0].titleId").value(4001))
                .andExpect(jsonPath("$.result.infos[0].code").value("TITLE-314"))
                .andExpect(jsonPath("$.result.infos[0].name").value("Trailblazer"))
                .andExpect(jsonPath("$.result.infos[0].category").value("CAREER"))
                .andExpect(jsonPath("$.result.infos[0].acquiredAt").value("2026-08-26T03:14:00Z"))
                .andExpect(jsonPath("$.result.infos[0].descMd").doesNotExist())
                .andExpect(jsonPath("$.result.infos[0].representative").doesNotExist());

        verify(holderQueryService).getTitleInfos(PLAYER_ID);
    }

    @Test
    @DisplayName("네 holder path는 positive playerId만 허용한다")
    void rejectsInvalidPlayerIds() throws Exception {
        mockMvc.perform(adminGet("/admin/v1/players/0/achievements"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(adminGet("/admin/v1/players/0/certifications"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(adminGet("/admin/v1/players/0/hobbies"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(adminGet("/admin/v1/players/0/titles"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(holderQueryService);
    }

    @Test
    @DisplayName("persisted USER authority는 holder read를 403으로 거부한다")
    void rejectsUser() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false))
        );

        mockMvc.perform(get("/admin/v1/players/314/titles")
                        .header("Authorization", bearer(USER_ID)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(holderQueryService);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    adminGet(String path) {
        return get(path).header("Authorization", bearer(ADMIN_ID));
    }

    private String bearer(long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId, null);
    }

    private static JwtProvider provider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiryMs(3_600_000L);
        properties.setRefreshTokenExpiryMs(604_800_000L);
        return new JwtProvider(properties);
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtProvider jwtProvider() {
            return provider();
        }
    }
}
