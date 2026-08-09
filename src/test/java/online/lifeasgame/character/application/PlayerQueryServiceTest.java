package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlayerQueryServiceTest {

    @Mock
    private PlayerReader playerReader;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @InjectMocks
    private PlayerQueryService playerQueryService;

    @Test
    void currentPlayerIdentityIsResolvedInsideQueryService() {
        Player player = Player.linkStart(23L, Name.of("player"), GenderType.MALE);
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(239L);
        given(playerReader.getByIdOrThrow(239L)).willReturn(player);

        assertThat(playerQueryService.getPlayerInfo().name()).isEqualTo("player");
        verify(playerReader).getByIdOrThrow(239L);
    }
}
