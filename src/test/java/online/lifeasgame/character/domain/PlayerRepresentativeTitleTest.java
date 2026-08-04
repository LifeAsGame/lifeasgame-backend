package online.lifeasgame.character.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerRepresentativeTitleTest {

    @Test
    void clearsMatchingRepresentativeTitle() {
        Player player = player();
        player.changeRepresentativeTitle(10L);

        player.clearRepresentativeTitleIfMatches(10L);

        assertThat(player.getTitleId()).isNull();
    }

    @Test
    void keepsDifferentRepresentativeTitle() {
        Player player = player();
        player.changeRepresentativeTitle(20L);

        player.clearRepresentativeTitleIfMatches(10L);

        assertThat(player.getTitleId()).isEqualTo(20L);
    }

    private Player player() {
        return Player.linkStart(1L, Name.of("player"), GenderType.MALE);
    }
}
