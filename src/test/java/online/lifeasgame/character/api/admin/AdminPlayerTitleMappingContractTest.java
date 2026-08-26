package online.lifeasgame.character.api.admin;

import online.lifeasgame.character.application.PlayerTitleService;
import online.lifeasgame.character.application.PlayerHolderQueryService;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.ControllerSliceTest;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

@ControllerSliceTest(controllers = AdminPlayerTitleController.class)
class AdminPlayerTitleMappingContractTest {

    private static final String PATH =
            "/admin/v1/players/{playerId}/titles/{titleId}";

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    private PlayerTitleService playerTitleService;

    @MockitoBean
    private PlayerHolderQueryService playerHolderQueryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    void exposesDeleteOnlyForRevokeAndKeepsPostGrant() {
        assertThat(hasMapping(RequestMethod.DELETE, PATH)).isTrue();
        assertThat(hasMapping(RequestMethod.GET, PATH)).isFalse();
        assertThat(hasMapping(RequestMethod.POST, PATH)).isTrue();
    }

    private boolean hasMapping(RequestMethod method, String path) {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(mapping ->
                        mapping.getMethodsCondition().getMethods().contains(method)
                                && mapping.getPatternValues().contains(path)
                );
    }
}
