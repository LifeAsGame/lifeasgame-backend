package online.lifeasgame.character.application;

import com.querydsl.jpa.impl.JPAQueryFactory;
import online.lifeasgame.character.application.query.GrowthQuery;
import online.lifeasgame.character.infra.growth.GrowthQueryAdapter;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.reward.application.internal.RewardGrowthSourceReadApi;
import online.lifeasgame.reward.infra.RewardGrowthSourceReadAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Growth cross-context architecture")
class GrowthArchitectureTest {

    @Nested
    @DisplayName("Character Growth read를 구성할 때")
    class CharacterBoundary {

        @Test
        @DisplayName("Reward Internal API만 의존하고 Reward entity/repository/infra를 참조하지 않는다")
        void dependsOnlyOnRewardInternalApi() {
            assertThat(fieldTypes(GrowthQueryService.class))
                    .containsExactlyInAnyOrder(
                            CurrentPlayerAccessor.class,
                            PlayerReader.class,
                            GrowthQuery.class,
                            RewardGrowthSourceReadApi.class
                    );
            Set<Class<?>> rewardDependencies = Arrays.stream(
                            GrowthQueryService.class.getDeclaredFields()
                    ).map(Field::getType)
                    .filter(type -> type.getPackageName().startsWith("online.lifeasgame.reward"))
                    .collect(Collectors.toSet());
            assertThat(rewardDependencies).containsExactly(RewardGrowthSourceReadApi.class);
            assertThat(fieldTypes(GrowthQueryAdapter.class)).containsExactly(JPAQueryFactory.class);
        }
    }

    @Nested
    @DisplayName("Reward provenance provider를 구성할 때")
    class RewardBoundary {

        @Test
        @DisplayName("Reward가 provider를 구현하며 Character Growth에 역의존하지 않는다")
        void remainsRewardOwnedWithoutReadCycle() {
            assertThat(RewardGrowthSourceReadApi.class
                    .isAssignableFrom(RewardGrowthSourceReadAdapter.class)).isTrue();
            assertThat(fieldTypes(RewardGrowthSourceReadAdapter.class))
                    .containsExactly(JPAQueryFactory.class);
            assertThat(Arrays.stream(RewardGrowthSourceReadAdapter.class.getDeclaredFields())
                    .map(Field::getType)
                    .noneMatch(type -> type.getPackageName().startsWith(
                            "online.lifeasgame.character"
                    ))).isTrue();
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
