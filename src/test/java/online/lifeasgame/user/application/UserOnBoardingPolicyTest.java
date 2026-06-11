package online.lifeasgame.user.application;

import online.lifeasgame.user.domain.event.UserRegistered;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserOnBoardingPolicyTest {

    @Mock UserSettingService userSettingService;
    @InjectMocks UserOnBoardingPolicy policy;

    @Test
    @DisplayName("UserRegistered → ensureDefaultIfMissing(userId) 호출")
    void on_callsEnsureDefault() {
        policy.on(UserRegistered.of(1L, "test@test.com", "Kirito"));
        verify(userSettingService).ensureDefaultIfMissing(1L);
    }

    @Test
    @DisplayName("userId가 올바르게 전달됨")
    void on_passesCorrectUserId() {
        policy.on(UserRegistered.of(42L, "other@test.com", "Hero"));
        verify(userSettingService).ensureDefaultIfMissing(42L);
    }
}
