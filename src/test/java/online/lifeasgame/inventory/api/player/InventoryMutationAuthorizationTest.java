package online.lifeasgame.inventory.api.player;

import online.lifeasgame.inventory.application.InventoryFacade;
import online.lifeasgame.inventory.application.MailboxFacade;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.ControllerSliceTest;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

@ControllerSliceTest(controllers = {
        InventoryController.class,
        MailboxController.class
})
@DisplayName("Player inventory mutation mapping contract")
class InventoryMutationAuthorizationTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    private InventoryFacade inventoryFacade;

    @MockitoBean
    private MailboxFacade mailboxFacade;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("Player item mint mapping은 등록되지 않는다")
    void removesPlayerMintMappings() {
        assertThat(hasMapping(RequestMethod.POST, "/api/v1/inventory/add"))
                .isFalse();
        assertThat(hasMapping(RequestMethod.POST, "/api/v1/mailbox/deliver"))
                .isFalse();
    }

    @Test
    @DisplayName("정상 Player inventory와 mailbox mapping은 유지한다")
    void keepsPlayerBehaviorMappings() {
        assertThat(hasMapping(RequestMethod.GET, "/api/v1/inventory"))
                .isTrue();
        assertThat(hasMapping(RequestMethod.PATCH, "/api/v1/inventory/move"))
                .isTrue();
        assertThat(hasMapping(RequestMethod.GET, "/api/v1/mailbox"))
                .isTrue();
        assertThat(hasMapping(RequestMethod.POST, "/api/v1/mailbox/claim"))
                .isTrue();
    }

    private boolean hasMapping(RequestMethod method, String path) {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(mapping ->
                        mapping.getMethodsCondition().getMethods().contains(method)
                                && mapping.getPatternValues().contains(path)
                );
    }
}
