package online.lifeasgame.character.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi.PlayerSummary;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
@DisplayName("Player connection batch read provider")
class PlayerConnectionReadIntegrationTest {

    @Autowired
    private PlayerConnectionReadApi playerConnectionReadApi;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("여러 Player의 connection summary를 한 query로 반환한다")
    void readsPlayerSummariesInOneQuery() {
        Player first = player(28401L, "첫 Player");
        Player second = player(28402L, "둘째 Player");
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = statistics();
        statistics.clear();

        Map<Long, PlayerSummary> result = playerConnectionReadApi
                .findAllByPlayerIds(Set.of(first.getId(), second.getId()));

        assertThat(result).containsOnlyKeys(first.getId(), second.getId());
        assertThat(result.get(first.getId())).isEqualTo(new PlayerSummary(
                first.getId(),
                "첫 Player",
                null,
                1
        ));
        assertThat(result.get(second.getId())).isEqualTo(new PlayerSummary(
                second.getId(),
                "둘째 Player",
                null,
                1
        ));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private Player player(Long userId, String name) {
        Player player = Player.linkStart(userId, Name.of(name), GenderType.MALE);
        entityManager.persist(player);
        return player;
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
