package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.LevelingPolicyParameters;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.event.PlayerLeveledUp;
import online.lifeasgame.character.domain.service.LevelingPolicy;
import online.lifeasgame.character.domain.service.PrecomputedLevelingPolicy;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerExpGrantService")
class PlayerExpGrantServiceTest {

    @Mock
    private PlayerReader playerReader;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private PlayerExpGrantService service;

    @BeforeEach
    void setUp() {
        LevelingPolicy levelingPolicy = new PrecomputedLevelingPolicy(new LevelingPolicyParameters(
                3,
                100L,
                List.of(new LevelingPolicyParameters.Bracket(1, 3, 1.0, 0L))
        ));
        service = new PlayerExpGrantService(playerReader, levelingPolicy, domainEventPublisher);
    }

    @Nested
    @DisplayName("Player EXP를 지급할 때")
    class GrantExperience {

        @Test
        @DisplayName("Player를 잠금 조회하고 지급 결과와 Level Up 이벤트를 발행한다")
        void locksPlayerAndPublishesLevelUpEvent() {
            Player player = player();
            given(playerReader.getByIdForUpdateOrThrow(1L)).willReturn(player);

            var result = service.grantExp(1L, 100L);

            assertThat(result.appliedExp()).isEqualTo(100L);
            assertThat(result.beforeLevel()).isEqualTo(1);
            assertThat(result.afterLevel()).isEqualTo(2);
            assertThat(result.totalExp()).isEqualTo(100L);
            verify(playerReader).getByIdForUpdateOrThrow(1L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<? extends DomainEvent>> events =
                    ArgumentCaptor.forClass(Collection.class);
            verify(domainEventPublisher).publishAll(events.capture());
            assertThat(events.getValue())
                    .singleElement()
                    .isInstanceOf(PlayerLeveledUp.class);
        }
    }

    private Player player() {
        Player player = Player.linkStart(10L, Name.of("테스터"), GenderType.MALE);
        ReflectionTestUtils.setField(player, "id", 1L);
        return player;
    }
}
