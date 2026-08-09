package online.lifeasgame.lifelog.quick.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.request.PlayerExerciseRequest;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.quick.application.QuickRecordResult;
import online.lifeasgame.lifelog.quick.application.QuickRecordService;
import online.lifeasgame.lifelog.quick.domain.error.QuickRecordError;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuickRecordController.class)
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class
})
@DisplayName("POST /api/v1/lifelogs/quick-record")
class QuickRecordControllerTest {

    private static final Instant RECORDED_AT =
            Instant.parse("2026-07-24T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuickRecordService quickRecordService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("인증이 없으면 401이다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(request("key-201", collectionRequest(), false))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Idempotency-Key가 없으면 400이다")
    void requiresIdempotencyKey() throws Exception {
        mockMvc.perform(request(null, collectionRequest(), true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("IDEM-400-KEY-REQUIRED"));
    }

    @Test
    @DisplayName("선택 subtype의 기존 Request validation을 적용한다")
    void validatesNestedSubtypeRequest() throws Exception {
        QuickRecordRequest.Create invalid =
                new QuickRecordRequest.Create(
                        "COLLECTION",
                        new PlayerCollectionRequest.Create(
                                "BOOK",
                                "title",
                                null,
                                0,
                                null,
                                null,
                                Set.of()
                        ),
                        null,
                        null
                );

        mockMvc.perform(request("invalid-subtype", invalid, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQ-VALIDATION"));
    }

    @Test
    @DisplayName("type과 payload 불일치는 400이다")
    void rejectsMismatchedTypeAndPayload() throws Exception {
        QuickRecordRequest.Create invalid =
                new QuickRecordRequest.Create(
                        "COLLECTION",
                        null,
                        exercisePayload(),
                        null
                );
        mockInvalid("mismatch-key");

        mockMvc.perform(request("mismatch-key", invalid, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("LIF-400-INVALID-QUICK-RECORD"));
    }

    @Test
    @DisplayName("두 subtype payload 동시 입력은 400이다")
    void rejectsMultiplePayloads() throws Exception {
        QuickRecordRequest.Create invalid =
                new QuickRecordRequest.Create(
                        "COLLECTION",
                        collectionPayload(),
                        exercisePayload(),
                        null
                );
        mockInvalid("multiple-key");

        mockMvc.perform(request("multiple-key", invalid, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("LIF-400-INVALID-QUICK-RECORD"));
    }

    @Test
    @DisplayName("subtype payload가 없으면 400이다")
    void rejectsMissingPayload() throws Exception {
        QuickRecordRequest.Create invalid =
                new QuickRecordRequest.Create(
                        "MEDIA",
                        null,
                        null,
                        null
                );
        mockInvalid("missing-payload-key");

        mockMvc.perform(request(
                        "missing-payload-key",
                        invalid,
                        true
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("LIF-400-INVALID-QUICK-RECORD"));
    }

    @Test
    @DisplayName("최초 생성은 실제 Source snapshot과 201을 반환한다")
    void returnsCreatedSource() throws Exception {
        when(quickRecordService.record(
                eq("created-key"),
                argThat(command -> command.collection() != null)
        )).thenReturn(result(false));

        mockMvc.perform(request(
                        "created-key",
                        collectionRequest(),
                        true
                ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.result.sourceType")
                        .value("COLLECTION"))
                .andExpect(jsonPath("$.result.sourceId").value(31))
                .andExpect(jsonPath("$.result.recordedAt")
                        .value("2026-07-24T15:00:00Z"))
                .andExpect(jsonPath("$.result.replay").value(false))
                .andExpect(jsonPath("$.result.sourceEventId")
                        .doesNotExist())
                .andExpect(jsonPath("$.result.lifeLogId")
                        .doesNotExist());
    }

    @Test
    @DisplayName("동일 요청 replay는 저장된 Source snapshot과 200을 반환한다")
    void returnsReplay() throws Exception {
        when(quickRecordService.record(
                eq("replay-key"),
                argThat(command -> command.collection() != null)
        )).thenReturn(result(true));

        mockMvc.perform(request(
                        "replay-key",
                        collectionRequest(),
                        true
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result.sourceType")
                        .value("COLLECTION"))
                .andExpect(jsonPath("$.result.sourceId").value(31))
                .andExpect(jsonPath("$.result.recordedAt")
                        .value("2026-07-24T15:00:00Z"))
                .andExpect(jsonPath("$.result.replay").value(true));
    }

    @Test
    @DisplayName("다른 payload conflict는 기존 Source나 private 값을 노출하지 않는다")
    void returnsConflictWithoutSensitiveResult() throws Exception {
        when(quickRecordService.record(
                eq("conflict-key"),
                argThat(command -> true)
        )).thenThrow(new DomainException(
                QuickRecordError.IDEMPOTENCY_KEY_PAYLOAD_CONFLICT
        ));

        mockMvc.perform(request(
                        "conflict-key",
                        collectionRequest(),
                        true
                ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("IDEM-409-KEY-PAYLOAD-CONFLICT"))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "Private collection title"
                                )
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "sourceId"
                                )
                        )
                ));
    }

    private void mockInvalid(String key) {
        when(quickRecordService.record(
                eq(key),
                argThat(command -> true)
        )).thenThrow(new DomainException(
                QuickRecordError.INVALID_REQUEST
        ));
    }

    private MockHttpServletRequestBuilder request(
            String idempotencyKey,
            QuickRecordRequest.Create request,
            boolean authenticated
    ) throws Exception {
        MockHttpServletRequestBuilder builder = post(
                "/api/v1/lifelogs/quick-record"
        )
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        if (authenticated) {
            builder.with(authentication(
                    new UsernamePasswordAuthenticationToken(
                            new JwtPrincipal(201L, 201001L),
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_USER"
                            ))
                    )
            ));
            builder.header("Authorization", "Bearer test-token");
        }
        return builder;
    }

    private QuickRecordRequest.Create collectionRequest() {
        return new QuickRecordRequest.Create(
                "COLLECTION",
                collectionPayload(),
                null,
                null
        );
    }

    private PlayerCollectionRequest.Create collectionPayload() {
        return new PlayerCollectionRequest.Create(
                "BOOK",
                "Private collection title",
                null,
                1,
                null,
                null,
                Set.of("private-tag")
        );
    }

    private PlayerExerciseRequest.Create exercisePayload() {
        return new PlayerExerciseRequest.Create(
                "RUNNING",
                30,
                5.0,
                250,
                LocalDate.of(2026, 7, 24),
                null
        );
    }

    private QuickRecordResult.Recorded result(boolean replay) {
        return new QuickRecordResult.Recorded(
                LifeLogType.COLLECTION,
                31L,
                RECORDED_AT,
                replay
        );
    }
}
