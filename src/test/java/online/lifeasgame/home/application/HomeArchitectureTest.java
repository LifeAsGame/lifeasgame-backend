package online.lifeasgame.home.application;

import online.lifeasgame.character.application.internal.AchievementProgressReadApi;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.home.api.HomeController;
import online.lifeasgame.lifelog.application.internal.LifeLogActivityReadApi;
import online.lifeasgame.quest.application.internal.QuestProgressReadApi;
import online.lifeasgame.role.application.internal.RoleDisplayReadApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Home read composition architecture")
class HomeArchitectureTest {

    @Nested
    @DisplayName("Home 경계를 구성할 때")
    class Boundaries {

        @Test
        @DisplayName("application은 current identity와 provider read API만 의존한다")
        void dependsOnlyOnReadBoundaries() {
            assertThat(fieldTypes(HomeQueryService.class))
                    .containsExactlyInAnyOrder(
                            CurrentPlayerAccessor.class,
                            Clock.class,
                            AchievementProgressReadApi.class,
                            LifeLogActivityReadApi.class,
                            QuestProgressReadApi.class,
                            RoleDisplayReadApi.class
                    );
            assertThat(fieldTypes(HomeController.class))
                    .containsExactly(HomeQueryService.class);
        }

        @Test
        @DisplayName("Home persistence와 write model은 존재하지 않는다")
        void createsNoHomePersistenceOrWriteModel() {
            for (String className : Set.of(
                    "online.lifeasgame.home.domain.Home",
                    "online.lifeasgame.home.domain.HomeEntity",
                    "online.lifeasgame.home.domain.HomeRepository",
                    "online.lifeasgame.home.application.HomeWriter"
            )) {
                assertThatThrownBy(() -> Class.forName(className))
                        .isInstanceOf(ClassNotFoundException.class);
            }
        }
    }

    private static Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getType)
                .collect(Collectors.toSet());
    }
}
