package online.lifeasgame.role.api;

import jakarta.validation.Validator;
import online.lifeasgame.person.api.PersonController;
import online.lifeasgame.person.api.mapper.PersonWebMapper;
import online.lifeasgame.person.api.request.PersonRequest;
import online.lifeasgame.person.application.PersonQueryService;
import online.lifeasgame.person.application.PersonService;
import online.lifeasgame.person.application.result.PersonResult;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.role.api.request.RoleRequest;
import online.lifeasgame.role.application.RoleQueryService;
import online.lifeasgame.role.application.RoleService;
import online.lifeasgame.support.ControllerSliceTest;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ControllerSliceTest(controllers = {RoleController.class, PersonController.class})
class RolePersonApiContractTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private Validator validator;

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
    void validatesRequiredFieldsAndKeepsOwnershipOutOfRequests() {
        assertThat(validator.validate(new RoleRequest.Create(" ", " ", null)))
                .hasSize(2);
        assertThat(validator.validate(new PersonRequest.Create(" ", null, null, null)))
                .hasSize(1);

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
}
