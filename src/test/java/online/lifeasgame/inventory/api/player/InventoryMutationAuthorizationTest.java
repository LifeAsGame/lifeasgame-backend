package online.lifeasgame.inventory.api.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.inventory.application.InventoryQueryService;
import online.lifeasgame.inventory.application.InventoryService;
import online.lifeasgame.inventory.application.MailboxQueryService;
import online.lifeasgame.inventory.application.MailboxService;
import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.api.player.mapper.InventoryWebMapper;
import online.lifeasgame.inventory.api.player.mapper.MailboxWebMapper;
import online.lifeasgame.inventory.api.player.request.InventoryRequest;
import online.lifeasgame.inventory.api.player.request.MailboxRequest;
import online.lifeasgame.inventory.api.player.spec.InventoryApiSpecV1;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.ControllerSliceTest;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = {
        InventoryController.class,
        MailboxController.class
})
@DisplayName("Player inventory mutation mapping contract")
class InventoryMutationAuthorizationTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private InventoryQueryService inventoryQueryService;

    @MockitoBean
    private MailboxService mailboxService;

    @MockitoBean
    private MailboxQueryService mailboxQueryService;

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

    @Test
    @DisplayName("discarded idempotency fields 없이 기존 Inventory와 Mailbox command를 만든다")
    void preservesCommandsWithoutIdempotencyFields() throws Exception {
        assertThat(componentNames(InventoryRequest.Remove.class))
                .containsExactly("slotIndex", "quantity");
        assertThat(componentNames(InventoryRequest.Move.class))
                .containsExactly("from", "to");
        assertThat(componentNames(InventoryRequest.Merge.class))
                .containsExactly("from", "to");
        assertThat(componentNames(InventoryRequest.Split.class))
                .containsExactly("from", "to", "quantity");
        assertThat(componentNames(MailboxRequest.Delete.class))
                .containsExactly("slotIndex");

        assertThat(InventoryWebMapper.toRemoveCommand(objectMapper.readValue(
                "{\"slotIndex\":1,\"quantity\":2}",
                InventoryRequest.Remove.class
        ))).isEqualTo(new InventoryCommand.Remove(1, 2));
        assertThat(InventoryWebMapper.toMoveCommand(objectMapper.readValue(
                "{\"from\":1,\"to\":2}",
                InventoryRequest.Move.class
        ))).isEqualTo(new InventoryCommand.Move(1, 2));
        assertThat(InventoryWebMapper.toMergeCommand(objectMapper.readValue(
                "{\"from\":2,\"to\":3}",
                InventoryRequest.Merge.class
        ))).isEqualTo(new InventoryCommand.Merge(2, 3));
        assertThat(InventoryWebMapper.toSplitCommand(objectMapper.readValue(
                "{\"from\":3,\"to\":4,\"quantity\":5}",
                InventoryRequest.Split.class
        ))).isEqualTo(new InventoryCommand.Split(3, 4, 5));
        assertThat(MailboxWebMapper.toDeleteCommand(objectMapper.readValue(
                "{\"slotIndex\":6}",
                MailboxRequest.Delete.class
        ))).isEqualTo(new MailboxCommand.Delete(6));
    }

    @Test
    @DisplayName("Inventory detail은 ignored query flag 없이 기존 응답 shape를 유지한다")
    void keepsDetailContractWithoutIgnoredFlags() throws Exception {
        assertSingleIdParameter(InventoryController.class);
        assertSingleIdParameter(InventoryApiSpecV1.class);
        InventoryResult.Entry entry = new InventoryResult.Entry(
                329L,
                2,
                32L,
                "Truth Item",
                "MATERIAL",
                "MATERIAL",
                "COMMON",
                true,
                99,
                4,
                false,
                null,
                Map.of("quality", "stable")
        );
        given(inventoryQueryService.getEntry(329L)).willReturn(entry);

        mockMvc.perform(get("/api/v1/inventory/329"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.entry.itemInstanceId")
                        .value(329))
                .andExpect(jsonPath("$.result.entry.slotIndex").value(2))
                .andExpect(jsonPath("$.result.entry.itemId").value(32))
                .andExpect(jsonPath("$.result.entry.itemName")
                        .value("Truth Item"))
                .andExpect(jsonPath("$.result.entry.instanceAttrs.quality")
                        .value("stable"));

        assertThat(InventoryWebMapper.toEntryDetail(entry).meta()).isNull();
        assertThat(InventoryWebMapper.toEntryDetail(entry).entry()
                .itemInstanceId()).isEqualTo(329L);
        verify(inventoryQueryService).getEntry(329L);
    }

    private boolean hasMapping(RequestMethod method, String path) {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(mapping ->
                        mapping.getMethodsCondition().getMethods().contains(method)
                                && mapping.getPatternValues().contains(path)
                );
    }

    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .toList();
    }

    private void assertSingleIdParameter(Class<?> type) {
        List<Method> methods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals("getEntry"))
                .toList();
        assertThat(methods).singleElement().satisfies(method ->
                assertThat(method.getParameterTypes())
                        .containsExactly(Long.class)
        );
    }
}
