package online.lifeasgame.role.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.role.api.request.RoleEventRequest;
import online.lifeasgame.role.application.RoleEventQueryService;
import online.lifeasgame.role.application.RoleEventService;
import online.lifeasgame.role.application.result.RoleEventResult;
import online.lifeasgame.support.ControllerSliceTest;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = RoleEventController.class)
@DisplayName("RoleEvent player API")
class RoleEventApiContractTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoleEventService service;

    @MockitoBean
    private RoleEventQueryService queryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("self RoleEvent endpoint를 노출할 때")
    class Endpoints {

        @Test
        @DisplayName("요청된 CRUD·complete·cancel·participant mapping만 제공한다")
        void exposesExactMappings() {
            String collection = "/api/v1/roles/{roleId}/events";
            String detail = collection + "/{eventId}";

            assertMapping(RequestMethod.GET, collection);
            assertMapping(RequestMethod.GET, detail);
            assertMapping(RequestMethod.POST, collection);
            assertMapping(RequestMethod.PATCH, detail);
            assertMapping(RequestMethod.POST, detail + "/complete");
            assertMapping(RequestMethod.POST, detail + "/cancel");
            assertMapping(RequestMethod.POST, detail + "/participants");
            assertMapping(
                    RequestMethod.DELETE,
                    detail + "/participants/{participantLinkId}"
            );
        }

        @Test
        @DisplayName("request body에 playerId를 받지 않는다")
        void keepsSelfIdentityOutOfRequests() {
            assertThat(componentNames(RoleEventRequest.Create.class))
                    .doesNotContain("playerId", "roleId", "eventId");
            assertThat(componentNames(RoleEventRequest.Update.class))
                    .doesNotContain("playerId", "roleId", "eventId");
            assertThat(componentNames(RoleEventRequest.AddParticipant.class))
                    .containsExactlyInAnyOrder(
                            "participantType",
                            "participantId"
                    )
                    .doesNotContain("playerId", "roleId", "eventId");
        }
    }

    @Nested
    @DisplayName("RoleEvent API를 호출하면")
    class DelegateUseCases {

        @Test
        @DisplayName("create/list/detail/update/상태 전이를 Application use case에 위임한다")
        void delegatesLifecycle() throws Exception {
            RoleEventResult.Detail detail = detail();
            given(service.create(eq(2L), any())).willReturn(detail);
            given(queryService.list(2L)).willReturn(List.of(detail));
            given(queryService.detail(2L, 5L)).willReturn(detail);
            given(service.update(eq(2L), eq(5L), any())).willReturn(detail);
            given(service.complete(2L, 5L)).willReturn(detail);
            given(service.cancel(2L, 5L)).willReturn(detail);

            mockMvc.perform(post("/api/v1/roles/2/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new RoleEventRequest.Create(
                                    "팀 회고", null, null, null
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.result.id").value(5L))
                    .andExpect(jsonPath("$.result.playerId").doesNotExist());
            mockMvc.perform(get("/api/v1/roles/2/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[0].status")
                            .value("PLANNED"));
            mockMvc.perform(get("/api/v1/roles/2/events/5"))
                    .andExpect(status().isOk());
            mockMvc.perform(patch("/api/v1/roles/2/events/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new RoleEventRequest.Update(
                                    "팀 회고", null, null, null
                            ))))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/roles/2/events/5/complete"))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/roles/2/events/5/cancel"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("participant link ID로 추가와 제거를 위임한다")
        void delegatesParticipantChanges() throws Exception {
            given(service.addParticipant(eq(2L), eq(5L), any()))
                    .willReturn(new RoleEventResult.Participant(
                            9L,
                            "PERSON",
                            3L
                    ));

            mockMvc.perform(post("/api/v1/roles/2/events/5/participants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(
                                    new RoleEventRequest.AddParticipant(
                                            "PERSON",
                                            3L
                                    )
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.participantLinkId")
                            .value(9L))
                    .andExpect(jsonPath("$.result.participantType")
                            .value("PERSON"));

            mockMvc.perform(delete(
                            "/api/v1/roles/2/events/5/participants/9"
                    ))
                    .andExpect(status().isNoContent());
            verify(service).removeParticipant(2L, 5L, 9L);
        }
    }

    private RoleEventResult.Detail detail() {
        return new RoleEventResult.Detail(
                5L,
                2L,
                "팀 회고",
                null,
                null,
                null,
                "PLANNED",
                null,
                List.of(),
                Instant.parse("2026-08-11T01:00:00Z"),
                Instant.parse("2026-08-11T01:00:00Z"),
                0L
        );
    }

    private void assertMapping(RequestMethod method, String path) {
        assertThat(handlerMapping.getHandlerMethods().keySet())
                .anyMatch(mapping ->
                        mapping.getMethodsCondition().getMethods()
                                .contains(method)
                                && mapping.getPatternValues().contains(path)
                );
    }

    private Set<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
