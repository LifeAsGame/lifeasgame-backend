package online.lifeasgame.user.api.user;

import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.ControllerSliceTest;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.user.application.UserQueryService;
import online.lifeasgame.user.application.UserService;
import online.lifeasgame.user.application.result.UserResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = UserController.class)
@DisplayName("User API")
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean UserQueryService userQueryService;
    @MockitoBean AppErrorProperties appErrorProperties;
    @MockitoBean ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class Me {

        @Test
        @DisplayName("Player가 없으면 non-null aggregate와 LINK_START를 반환한다")
        void withoutPlayer() throws Exception {
            given(userQueryService.getUserInfo()).willReturn(new UserResult.UserInfo(
                    1L,
                    "user@example.com",
                    "PlayerOne",
                    "ACTIVE",
                    null
            ));

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.user.id").value(1))
                    .andExpect(jsonPath("$.result.user.email").value("user@example.com"))
                    .andExpect(jsonPath("$.result.user.nickname").value("PlayerOne"))
                    .andExpect(jsonPath("$.result.user.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.result.player.exists").value(false))
                    .andExpect(jsonPath("$.result.ui.nextActions[0]").value("LINK_START"))
                    .andExpect(jsonPath("$.result.ui.nextActions.length()").value(1))
                    .andExpect(jsonPath("$.result.ui.badges.notifications").value(0))
                    .andExpect(jsonPath("$.result.ui.badges.pendingRewards").value(0));
        }

        @Test
        @DisplayName("Player가 연결되어 있으면 실제 playerId와 빈 nextActions를 반환한다")
        void withPlayer() throws Exception {
            given(userQueryService.getUserInfo()).willReturn(new UserResult.UserInfo(
                    1L,
                    "user@example.com",
                    "PlayerOne",
                    "ACTIVE",
                    77L
            ));

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.user").exists())
                    .andExpect(jsonPath("$.result.player.exists").value(true))
                    .andExpect(jsonPath("$.result.player.playerId").value(77))
                    .andExpect(jsonPath("$.result.ui").exists())
                    .andExpect(jsonPath("$.result.ui.nextActions").isEmpty());
        }
    }
}
