package online.lifeasgame.quest.api.player;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.quest.application.QuestFacade;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.QuestTargetType;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestController.class)
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class
})
@DisplayName("QuestController manual-check")
class QuestControllerQuestManualCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestFacade questFacade;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("Request body 없이 기존 Acceptance DTO를 200으로 반환한다")
    void returnsAcceptanceWithoutBody() throws Exception {
        when(questFacade.manualCheck(
                new QuestCommand.ManualCheck("Q_GROWTH_ONE_FOCUS")
        )).thenReturn(acceptance());

        mockMvc.perform(authenticatedPost("Q_GROWTH_ONE_FOCUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result.id").value(21700))
                .andExpect(jsonPath("$.result.code")
                        .value("Q_GROWTH_ONE_FOCUS"))
                .andExpect(jsonPath("$.result.targetValue").value(25))
                .andExpect(jsonPath("$.result.progressValue").value(25))
                .andExpect(jsonPath("$.result.status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.result.completedAt")
                        .value("2026-07-31T03:00:00Z"));

        verify(questFacade).manualCheck(
                new QuestCommand.ManualCheck("Q_GROWTH_ONE_FOCUS")
        );
    }

    @Test
    @DisplayName("허용하지 않은 Quest는 안정된 409를 반환한다")
    void rejectsWrongQuest() throws Exception {
        when(questFacade.manualCheck(argThat(command ->
                command.questCode().equals("Q_RECORD_FIRST_TRACE")
        ))).thenThrow(new DomainException(
                QuestError.QUEST_MANUAL_CHECK_NOT_ALLOWED
        ));

        mockMvc.perform(authenticatedPost("Q_RECORD_FIRST_TRACE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("QUE-409-QUEST-MANUAL-CHECK-NOT-ALLOWED"));
    }

    private MockHttpServletRequestBuilder authenticatedPost(
            String questCode
    ) {
        return post(
                "/api/v1/players/quests/{questCode}/manual-check",
                questCode
        )
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(
                                new JwtPrincipal(217L, 217001L),
                                null,
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_USER"
                                ))
                        )
                ))
                .header("Authorization", "Bearer test-token");
    }

    private QuestResult.Acceptance acceptance() {
        Instant checkedAt = Instant.parse("2026-07-31T03:00:00Z");
        return new QuestResult.Acceptance(
                21700L,
                2170L,
                217001L,
                "Q_GROWTH_ONE_FOCUS",
                "한 가지에 25분 집중하기",
                null,
                "manual check",
                QuestTargetType.MINUTES,
                25,
                25,
                "COMPLETED",
                "USER_CONFIRM",
                "DAILY",
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 7, 31),
                Instant.parse("2026-07-31T01:00:00Z"),
                null,
                checkedAt,
                checkedAt,
                null,
                "GROWTH",
                "MANUAL_CHECK",
                "DAILY",
                null
        );
    }
}
