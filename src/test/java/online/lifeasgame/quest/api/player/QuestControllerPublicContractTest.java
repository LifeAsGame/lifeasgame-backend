package online.lifeasgame.quest.api.player;

import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.quest.api.player.request.QuestRequest;
import online.lifeasgame.quest.application.QuestManualCheckService;
import online.lifeasgame.quest.application.QuestQueryService;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.QuestTargetType;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Quest public accept/cancel contract")
class QuestControllerPublicContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestService questService;

    @MockitoBean
    private QuestQueryService questQueryService;

    @MockitoBean
    private QuestManualCheckService questManualCheckService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("Accept와 Cancel DTO는 dead idempotencyKey를 노출하지 않는다")
    void exposesOnlySupportedFields() {
        assertThat(componentNames(QuestRequest.Accept.class))
                .containsExactly("partyId", "guildId");
        assertThat(componentNames(QuestRequest.Cancel.class))
                .containsExactly("reason");
    }

    @Test
    @DisplayName("Accept는 명시적 null과 omitted context를 null command로 전달한다")
    void acceptsNullAndOmittedContext() throws Exception {
        when(questService.accept(any(QuestCommand.Accept.class)))
                .thenReturn(acceptance());

        mockMvc.perform(accept("{\"partyId\":null,\"guildId\":null}"))
                .andExpect(status().isCreated());
        mockMvc.perform(accept("{}"))
                .andExpect(status().isCreated());

        verify(questService, times(2)).accept(new QuestCommand.Accept(
                "PLAYER_WELCOME",
                null,
                null
        ));
    }

    @Test
    @DisplayName("non-null partyId와 guildId는 mutation 전에 400으로 거부한다")
    void rejectsLegacyContext() throws Exception {
        mockMvc.perform(accept("{\"partyId\":3271}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(accept("{\"guildId\":3272}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(questService);
    }

    @Test
    @DisplayName("Cancel은 optional reason을 application command에 보존한다")
    void preservesCancelReason() throws Exception {
        when(questService.cancel(any(QuestCommand.Cancel.class)))
                .thenReturn(new QuestResult.Canceled(
                        327L,
                        32L,
                        "PLAYER_WELCOME"
                ));

        mockMvc.perform(delete(
                        "/api/v1/players/quests/PLAYER_WELCOME"
                )
                        .with(playerAuthentication())
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"현재 우선순위 변경\"}"))
                .andExpect(status().isNoContent());

        verify(questService).cancel(new QuestCommand.Cancel(
                "PLAYER_WELCOME",
                "현재 우선순위 변경"
        ));
    }

    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .toList();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    accept(String body) {
        return post("/api/v1/players/quests/PLAYER_WELCOME")
                .with(playerAuthentication())
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private RequestPostProcessor playerAuthentication() {
        return authentication(new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(327L, 32701L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    private QuestResult.Acceptance acceptance() {
        Instant acceptedAt = Instant.parse("2026-09-03T01:00:00Z");
        return new QuestResult.Acceptance(
                3270L,
                32L,
                327L,
                "PLAYER_WELCOME",
                "Welcome",
                "MAIN",
                null,
                QuestTargetType.COUNT,
                1,
                0,
                "IN_PROGRESS",
                "USER_CONFIRM",
                "ONCE",
                LocalDate.of(1970, 1, 1),
                LocalDate.of(9999, 12, 31),
                acceptedAt,
                null,
                null,
                null,
                null,
                "GROWTH",
                "MANUAL_CHECK",
                "ONCE",
                null
        );
    }
}
