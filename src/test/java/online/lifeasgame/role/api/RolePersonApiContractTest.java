package online.lifeasgame.role.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.person.api.PersonController;
import online.lifeasgame.person.api.mapper.PersonWebMapper;
import online.lifeasgame.person.api.request.PersonRequest;
import online.lifeasgame.person.application.PersonQueryService;
import online.lifeasgame.person.application.PersonService;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.application.result.PersonResult;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.role.api.request.RoleRequest;
import online.lifeasgame.role.application.RoleQueryService;
import online.lifeasgame.role.application.RoleService;
import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.role.application.result.RoleResult;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleType;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = {RoleController.class, PersonController.class})
class RolePersonApiContractTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private RoleQueryService roleQueryService;

    @MockitoBean
    private PersonService personService;

    @MockitoBean
    private PersonQueryService personQueryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    void exposesOnlyRequestedCrudMappings() {
        assertMapping(RequestMethod.POST, "/api/v1/roles");
        assertMapping(RequestMethod.GET, "/api/v1/roles");
        assertMapping(RequestMethod.GET, "/api/v1/roles/{roleId}");
        assertMapping(RequestMethod.PUT, "/api/v1/roles/{roleId}");
        assertMapping(RequestMethod.DELETE, "/api/v1/roles/{roleId}");
        assertMapping(RequestMethod.POST, "/api/v1/persons");
        assertMapping(RequestMethod.GET, "/api/v1/persons");
        assertMapping(RequestMethod.GET, "/api/v1/persons/{personId}");
        assertMapping(RequestMethod.PUT, "/api/v1/persons/{personId}");
        assertMapping(RequestMethod.DELETE, "/api/v1/persons/{personId}");
    }

    @Test
    void keepsOwnershipOutOfRequests() {
        assertThat(componentNames(RoleRequest.Create.class))
                .doesNotContain("playerId", "ownerPlayerId");
        assertThat(componentNames(RoleRequest.Update.class))
                .doesNotContain("playerId", "ownerPlayerId");
        assertThat(componentNames(PersonRequest.Create.class))
                .doesNotContain("playerId", "ownerPlayerId", "linkedUserId");
        assertThat(componentNames(PersonRequest.Update.class))
                .doesNotContain("playerId", "ownerPlayerId", "linkedUserId");
    }

    @Test
    void returnsRoleDomainValidationCodesFromHttpRequests() throws Exception {
        given(roleService.create(any())).willAnswer(invocation -> {
            RoleCommand.Create command = invocation.getArgument(0);
            Role.create(
                    234L,
                    RoleType.of(command.roleType()),
                    command.name(),
                    command.description()
            );
            return roleDetail(10L);
        });

        assertRoleError(new RoleRequest.Create(null, "Self", null), RoleError.INVALID_ROLE_TYPE);
        assertRoleError(new RoleRequest.Create("SELF", "   ", null), RoleError.INVALID_ROLE_NAME);
        assertRoleError(
                new RoleRequest.Create("SELF", "Self", "x".repeat(501)),
                RoleError.INVALID_ROLE_DESCRIPTION
        );
    }

    @Test
    void returnsPersonDomainValidationCodesFromHttpRequests() throws Exception {
        given(personService.create(any())).willAnswer(invocation -> {
            PersonCommand.Create command = invocation.getArgument(0);
            Person.create(
                    234L,
                    command.displayName(),
                    command.notes(),
                    command.birthday(),
                    command.contact()
            );
            return personDetail(20L);
        });

        assertPersonError(
                new PersonRequest.Create(null, null, null, null),
                PersonError.INVALID_PERSON_DISPLAY_NAME
        );
        assertPersonError(
                new PersonRequest.Create("Alice", null, null, "x".repeat(121)),
                PersonError.INVALID_PERSON_CONTACT
        );
    }

    @Test
    void acceptsTrimmedBoundaryInputsAndKeepsSuccessResponseShape() throws Exception {
        given(roleService.create(any())).willReturn(roleDetail(10L));
        given(personService.create(any())).willReturn(personDetail(20L));
        given(roleService.update(eq(10L), any())).willReturn(roleDetail(10L));
        given(personService.update(eq(20L), any())).willReturn(personDetail(20L));

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RoleRequest.Create(
                                "  " + "r".repeat(40) + "  ",
                                "  " + "n".repeat(60) + "  ",
                                "  " + "d".repeat(500) + "  "
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.id").value(10L))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PersonRequest.Create(
                                "  " + "n".repeat(80) + "  ",
                                null,
                                null,
                                "  " + "c".repeat(120) + "  "
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.id").value(20L))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));

        verify(roleService).create(argThat(command -> command.name().strip().length() == 60));
        verify(personService).create(argThat(command -> command.displayName().strip().length() == 80));

        mockMvc.perform(put("/api/v1/roles/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RoleRequest.Update("SELF", "Self", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(10L))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));

        mockMvc.perform(put("/api/v1/persons/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PersonRequest.Update("Alice", null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(20L))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));
    }

    @Test
    void deleteDelegatesExactlyOnceAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/10"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/persons/20"))
                .andExpect(status().isNoContent());

        verify(roleService).archive(10L);
        verify(personService).archive(20L);
    }

    @Test
    void keepsLinkedUserReadMappingWithoutWriteInput() {
        var response = PersonWebMapper.toDetail(new PersonResult.Detail(
                1L,
                2L,
                3L,
                "Alice",
                null,
                LocalDate.of(2000, 1, 1),
                null,
                "ACTIVE",
                null,
                null,
                0L
        ));

        assertThat(response.linkedUserId()).isEqualTo(3L);
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

    private void assertRoleError(RoleRequest.Create request, RoleError error) throws Exception {
        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(error.code()));
    }

    private void assertPersonError(PersonRequest.Create request, PersonError error) throws Exception {
        mockMvc.perform(post("/api/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(error.code()));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private RoleResult.Detail roleDetail(Long id) {
        return new RoleResult.Detail(
                id, 234L, "SELF", "Self", null, "ACTIVE", null, null, 0L
        );
    }

    private PersonResult.Detail personDetail(Long id) {
        return new PersonResult.Detail(
                id, 234L, null, "Alice", null, null, null, "ACTIVE", null, null, 0L
        );
    }
}
