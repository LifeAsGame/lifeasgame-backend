package online.lifeasgame.character.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.InventoryQueryService;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentReadApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Equipment cross-context architecture")
class EquipmentArchitectureTest {

    @Nested
    @DisplayName("Character equipment write를 구성할 때")
    class CharacterBoundary {

        @Test
        @DisplayName("Inventory provider Internal API만 의존한다")
        void dependsOnlyOnInventoryProviderBoundary() {
            assertThat(fieldTypes(PlayerEquipmentService.class))
                    .containsExactlyInAnyOrder(
                            PlayerEquipmentWriter.class,
                            PlayerEquipmentReader.class,
                            EquipmentSlotReader.class,
                            InventoryEquipmentReadApi.class,
                            CurrentPlayerAccessor.class
                    );
            Set<Class<?>> inventoryDependencies = Arrays.stream(
                            PlayerEquipmentService.class.getDeclaredFields()
                    ).map(Field::getType)
                    .filter(type -> type.getPackageName().startsWith(
                            "online.lifeasgame.inventory"
                    ))
                    .collect(Collectors.toSet());
            assertThat(inventoryDependencies)
                    .containsExactlyInAnyOrder(InventoryEquipmentReadApi.class);
        }
    }

    @Nested
    @DisplayName("Inventory equipment provider를 구성할 때")
    class InventoryBoundary {

        @Test
        @DisplayName("기존 Inventory query service가 Character 의존 없이 구현한다")
        void remainsProviderOwnedWithoutCycle() {
            assertThat(InventoryEquipmentReadApi.class
                    .isAssignableFrom(InventoryQueryService.class)).isTrue();
            assertThat(Arrays.stream(
                            InventoryQueryService.class.getDeclaredFields()
                    ).map(Field::getType)
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
