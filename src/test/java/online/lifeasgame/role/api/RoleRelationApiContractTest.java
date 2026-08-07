package online.lifeasgame.role.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.role.api.request.RoleRelationRequest;
import online.lifeasgame.role.application.RoleRelationQueryService;
import online.lifeasgame.role.application.RoleRelationService;
import online.lifeasgame.role.application.command.RoleRelationCommand;
import online.lifeasgame.role.application.result.RoleRelationResult;
import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.RoleRelationType;
import online.lifeasgame.role.domain.error.RoleError;
import online.lifeasgame.support.ControllerSliceTest;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.RecordComponent;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = RoleRelationController.class)
class RoleRelationApiContractTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoleRelationService relationService;

    @MockitoBean
    private RoleRelationQueryService relationQueryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    void exposesExactCrudMappings() {
        String collection = "/api/v1/roles/{roleId}/relations";
        String detail = collection + "/{relationId}";

        assertMapping(RequestMethod.POST, collection);
        assertMapping(RequestMethod.GET, collection);
        assertMapping(RequestMethod.GET, detail);
        assertMapping(RequestMethod.PUT, detail);
        assertMapping(RequestMethod.DELETE, detail);
    }

    @Test
    void keepsOwnerAndImmutableIdentityOutOfRequests() {
        assertThat(componentNames(RoleRelationRequest.Create.class))
                .containsExactlyInAnyOrder("personId", "relationType", "roleNotes")
                .doesNotContain("playerId", "ownerPlayerId", "roleId");
        assertThat(componentNames(RoleRelationRequest.Update.class))
                .containsExactlyInAnyOrder("relationType", "roleNotes")
                .doesNotContain("personId", "playerId", "ownerPlayerId", "roleId");
    }

    @Test
    void servesPostGetPutDeleteWithoutOwnerIdentity() throws Exception {
        RoleRelationResult.Detail detail = detail();
        given(relationService.create(eq(2L), any())).willReturn(detail);
        given(relationQueryService.list(2L)).willReturn(List.of(detail));
        given(relationQueryService.detail(2L, 9L)).willReturn(detail);
        given(relationService.update(eq(2L), eq(9L), any())).willReturn(detail);

        mockMvc.perform(post("/api/v1/roles/2/relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RoleRelationRequest.Create(
                                3L,
                                "FAMILY",
                                "note"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.id").value(9L))
                .andExpect(jsonPath("$.result.personDisplayName").value("Alice"))
                .andExpect(jsonPath("$.result.linkedUserId").value(4L))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.playerId").doesNotExist())
                .andExpect(jsonPath("$.result.ownerPlayerId").doesNotExist())
                .andExpect(jsonPath("$.result.roleId").doesNotExist());

        mockMvc.perform(get("/api/v1/roles/2/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(9L));

        mockMvc.perform(get("/api/v1/roles/2/relations/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.personId").value(3L));

        mockMvc.perform(put("/api/v1/roles/2/relations/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RoleRelationRequest.Update("FRIEND", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(9L))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/v1/roles/2/relations/9"))
                .andExpect(status().isNoContent());
        verify(relationService).archive(2L, 9L);
    }

    @Test
    void returnsDomainCodesForInvalidTypeAndDuplicate() throws Exception {
        given(relationService.create(eq(2L), any())).willAnswer(invocation -> {
            RoleRelationCommand.Create command = invocation.getArgument(1);
            RoleRelation.create(
                    1L,
                    2L,
                    command.personId(),
                    RoleRelationType.of(command.relationType()),
                    command.roleNotes()
            );
            return detail();
        });

        mockMvc.perform(post("/api/v1/roles/2/relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RoleRelationRequest.Create(3L, "   ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(
                        RoleError.INVALID_ROLE_RELATION_TYPE.code()
                ));

        given(relationService.create(eq(2L), any())).willThrow(
                new DomainException(RoleError.ROLE_RELATION_ALREADY_EXISTS)
        );
        mockMvc.perform(post("/api/v1/roles/2/relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RoleRelationRequest.Create(3L, "FAMILY", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(
                        RoleError.ROLE_RELATION_ALREADY_EXISTS.code()
                ));
    }

    private RoleRelationResult.Detail detail() {
        return new RoleRelationResult.Detail(
                9L,
                1L,
                2L,
                3L,
                "Alice",
                4L,
                "FAMILY",
                "note",
                "ACTIVE",
                null,
                null,
                0L
        );
    }

    private void assertMapping(RequestMethod method, String path) {
        assertThat(handlerMapping.getHandlerMethods().keySet()).anyMatch(mapping ->
                mapping.getMethodsCondition().getMethods().contains(method)
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
