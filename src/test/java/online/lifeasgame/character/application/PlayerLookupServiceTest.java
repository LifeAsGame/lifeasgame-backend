package online.lifeasgame.character.application;

import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.character.domain.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlayerLookupServiceTest {

    @Mock
    private PlayerReader playerReader;

    @InjectMocks
    private PlayerLookupService playerLookupService;

    @Test
    void exposesOnlyPlayerIdForUserLookup() {
        Player player = org.mockito.Mockito.mock(Player.class);
        given(player.getId()).willReturn(239L);
        given(playerReader.getByUserId(23L)).willReturn(player);

        PlayerLookupApi api = playerLookupService;

        assertThat(api.findPlayerIdByUserId(23L)).isEqualTo(239L);
    }

    @Test
    void returnsNullBeforePlayerOnboarding() {
        given(playerReader.getByUserId(23L)).willReturn(null);

        assertThat(playerLookupService.findPlayerIdByUserId(23L)).isNull();
    }
}
