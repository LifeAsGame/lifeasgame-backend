package online.lifeasgame.character.api.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.character.application.PlayerEquipmentService;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerEquipmentController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Player equipment API contract")
class PlayerEquipmentApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlayerEquipmentService playerEquipmentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("기존 equipment endpoint를 호출할 때")
    class ExistingContract {

        @Test
        @DisplayName("GET, PUT, DELETE shape와 current-player 요청을 그대로 유지한다")
        void preservesExistingSuccessShapes() throws Exception {
            given(playerEquipmentService.getPlayerEquipmentInfos())
                    .willReturn(List.of(new PlayerEquipmentResult.Info(
                            1L,
                            "WEAPON_MAIN",
                            "주무기",
                            "WEAPON",
                            "MAIN",
                            101L
                    )));
            given(playerEquipmentService.equip(any()))
                    .willReturn(new PlayerEquipmentResult.Equipped(1L, 102L));

            mockMvc.perform(get("/api/v1/players/equipment")
                            .with(authentication(playerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.infos[0].slotId")
                            .value(1))
                    .andExpect(jsonPath("$.result.infos[0].slotCode")
                            .value("WEAPON_MAIN"))
                    .andExpect(jsonPath("$.result.infos[0].slotName")
                            .value("주무기"))
                    .andExpect(jsonPath("$.result.infos[0].slotCategory")
                            .value("WEAPON"))
                    .andExpect(jsonPath("$.result.infos[0].slotRole")
                            .value("MAIN"))
                    .andExpect(jsonPath("$.result.infos[0].itemInstanceId")
                            .value(101));

            mockMvc.perform(put("/api/v1/players/equipment/1")
                            .with(authentication(playerAuthentication()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new online.lifeasgame.character.api.player.request.PlayerEquipmentRequest.Equip(
                                    102L
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.slotId").value(1))
                    .andExpect(jsonPath("$.result.itemInstanceId")
                            .value(102));

            mockMvc.perform(delete("/api/v1/players/equipment/1")
                            .with(authentication(playerAuthentication())))
                    .andExpect(status().isNoContent());

            assertThat(Arrays.stream(PlayerEquipmentCommand.Equip.class
                            .getRecordComponents())
                    .map(RecordComponent::getName))
                    .containsExactly("slotId", "itemInstanceId")
                    .doesNotContain("playerId", "userId");
            verify(playerEquipmentService).unEquip(1L);
        }

        @Test
        @DisplayName("인증이 없으면 모든 write가 401이다")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(put("/api/v1/players/equipment/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"itemInstanceId\":102}"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(delete("/api/v1/players/equipment/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("authoritative invariant를 위반하면")
    class InvariantFailure {

        @Test
        @DisplayName("foreign과 nonexistent item은 같은 ownership-safe code다")
        void returnsOwnershipSafeNotFound() throws Exception {
            given(playerEquipmentService.equip(any())).willThrow(
                    new DomainException(
                            InventoryError.INVENTORY_ENTRY_NOT_FOUND
                    )
            );

            mockMvc.perform(equipRequest())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(
                            InventoryError.INVENTORY_ENTRY_NOT_FOUND.code()
                    ));
        }

        @Test
        @DisplayName("precheck와 DB race는 같은 already-equipped code다")
        void returnsStableDuplicateConflict() throws Exception {
            given(playerEquipmentService.equip(any())).willThrow(
                    new DomainException(
                            PlayerEquipmentError.ALREADY_EQUIPPED_ITEM
                    )
            );

            mockMvc.perform(equipRequest())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(
                            PlayerEquipmentError.ALREADY_EQUIPPED_ITEM.code()
                    ));
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    equipRequest() throws Exception {
        return put("/api/v1/players/equipment/1")
                .with(authentication(playerAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new online.lifeasgame.character.api.player.request.PlayerEquipmentRequest.Equip(
                        102L
                )));
    }

    private UsernamePasswordAuthenticationToken playerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(262L, 26201L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
