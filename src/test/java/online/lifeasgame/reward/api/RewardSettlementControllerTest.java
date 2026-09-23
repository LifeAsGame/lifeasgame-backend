package online.lifeasgame.reward.api;

import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.reward.application.RewardSettlementQueryService;
import online.lifeasgame.reward.application.result.RewardSettlementResult;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import online.lifeasgame.reward.domain.RewardType;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RewardSettlementController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Reward Settlement player API")
class RewardSettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardSettlementQueryService queryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("Quest 완료 Settlement를 조회할 때")
    class GetQuestCompletionSettlement {

        @Test
        @DisplayName("저장된 Settlement와 Line 처리 상태를 반환한다")
        void returnsSettlementState() throws Exception {
            when(queryService.getQuestCompletionSettlement(73L))
                    .thenReturn(detail());

            mockMvc.perform(authenticated(get(
                            "/api/v1/reward-settlements/quest-completions/{questAcceptanceId}",
                            73L
                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.settlementId").value(91L))
                    .andExpect(jsonPath("$.result.sourceType")
                            .value("QUEST_COMPLETION"))
                    .andExpect(jsonPath("$.result.sourceId").value(73L))
                    .andExpect(jsonPath("$.result.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.result.lines[0].rewardType")
                            .value("ITEM"))
                    .andExpect(jsonPath("$.result.lines[0].amount").value(2L))
                    .andExpect(jsonPath("$.result.lines[0].status")
                            .value("SUCCEEDED"))
                    .andExpect(jsonPath("$.result.lines[0].deliveryState")
                            .doesNotExist())
                    .andExpect(jsonPath("$.result.lines[0].owned")
                            .doesNotExist())
                    .andExpect(jsonPath("$.result.playerId")
                            .doesNotExist());

            verify(queryService).getQuestCompletionSettlement(73L);
        }

        @Test
        @DisplayName("인증되지 않은 요청은 Query Service를 호출하지 않는다")
        void rejectsUnauthenticatedRequest() throws Exception {
            mockMvc.perform(get(
                            "/api/v1/reward-settlements/quest-completions/{questAcceptanceId}",
                            73L
                    ))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(queryService);
        }
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request
    ) {
        return request.with(authentication(
                        new UsernamePasswordAuthenticationToken(
                                new JwtPrincipal(42L, 42001L),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                ))
                .header("Authorization", "Bearer test-token");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = RewardSettlementStatus.class,
            names = {"PENDING", "PARTIAL_FAILED", "NOT_ELIGIBLE"})
    @DisplayName("GOLD 금액과 미완료 정산 상태를 성공 완료로 바꾸지 않는다")
    void exposesUnfinishedGold(RewardSettlementStatus settlementStatus) throws Exception {
        var now = Instant.now();
        var lines = settlementStatus == RewardSettlementStatus.NOT_ELIGIBLE
                ? List.<RewardSettlementResult.Line>of()
                : List.of(new RewardSettlementResult.Line(1L, 1L, "RD_ADVENTURE_GOLD", RewardType.GOLD,
                        100L, null, null, 1, RewardSettlementLineStatus.SUCCEEDED, null, now, now),
                    new RewardSettlementResult.Line(2L, 2L, "RD_RECORD_CRYSTAL", RewardType.ITEM,
                        1L, 2L, "IT_RECORD_CRYSTAL", 2,
                        settlementStatus == RewardSettlementStatus.PENDING
                                ? RewardSettlementLineStatus.PENDING : RewardSettlementLineStatus.FAILED,
                        settlementStatus == RewardSettlementStatus.PENDING ? null : "INV-MAILBOX-FULL", now, now));
        when(queryService.getQuestCompletionSettlement(73L)).thenReturn(new RewardSettlementResult.Detail(
                91L, RewardSettlementSourceType.QUEST_COMPLETION, 73L, 1L, "RP_ADVENTURE_PREPARATION",
                settlementStatus, now, now, lines));
        var response = mockMvc.perform(authenticated(get(
                "/api/v1/reward-settlements/quest-completions/73")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value(settlementStatus.name()));
        if (!lines.isEmpty()) {
            response.andExpect(jsonPath("$.result.lines[0].rewardType").value("GOLD"))
                    .andExpect(jsonPath("$.result.lines[0].amount").value(100))
                    .andExpect(jsonPath("$.result.lines[1].itemCode").value("IT_RECORD_CRYSTAL"))
                    .andExpect(jsonPath("$.result.lines[1].status").value(lines.get(1).status().name()));
        } else {
            response.andExpect(jsonPath("$.result.lines").isEmpty());
        }
    }

    private RewardSettlementResult.Detail detail() {
        Instant createdAt = Instant.parse("2026-09-15T01:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-15T01:01:00Z");
        return new RewardSettlementResult.Detail(
                91L,
                RewardSettlementSourceType.QUEST_COMPLETION,
                73L,
                101L,
                "RP_ITEM_STARTER",
                RewardSettlementStatus.COMPLETED,
                createdAt,
                updatedAt,
                List.of(new RewardSettlementResult.Line(
                        92L,
                        102L,
                        "RD_ITEM_STARTER",
                        RewardType.ITEM,
                        2L,
                        103L,
                        "IT_STARTER",
                        0,
                        RewardSettlementLineStatus.SUCCEEDED,
                        null,
                        createdAt,
                        updatedAt
                ))
        );
    }
}
