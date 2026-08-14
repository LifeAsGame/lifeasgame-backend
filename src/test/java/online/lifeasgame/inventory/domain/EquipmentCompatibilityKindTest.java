package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.ItemError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Item equipment compatibility kind")
class EquipmentCompatibilityKindTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("validCompatibilityPairs")
    @DisplayName("kind와 category의 canonical 조합을 허용한다")
    void allowsCanonicalCategory(
            EquipmentCompatibilityKind kind,
            ItemCategory category
    ) {
        Item item = item(category, kind);

        assertThat(item.getEquipmentCompatibilityKind()).isEqualTo(kind);
    }

    @ParameterizedTest(name = "{0} + {1}")
    @MethodSource("invalidCompatibilityPairs")
    @DisplayName("kind와 category가 맞지 않으면 기존 Item 상태를 바꾸지 않고 거부한다")
    void rejectsInvalidCategoryWithoutMutation(
            EquipmentCompatibilityKind kind,
            ItemCategory category
    ) {
        Item item = item(ItemCategory.MISC, null);

        assertThatThrownBy(() -> item.update(
                ItemName.of("변경된 Item"),
                category,
                ItemType.ETC,
                kind,
                Rarity.RARE,
                BaseAttrs.empty(),
                false,
                null,
                null
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(ItemError.INVALID_EQUIPMENT_COMPATIBILITY)
        );

        assertThat(item.getName().value()).isEqualTo("Item");
        assertThat(item.getCategory()).isEqualTo(ItemCategory.MISC);
        assertThat(item.getEquipmentCompatibilityKind()).isNull();
    }

    @Test
    @DisplayName("null kind는 legacy 정의로 허용하고 기존 explicit kind를 지운다")
    void allowsNullKindAndClearsExplicitKind() {
        assertThat(item(ItemCategory.MISC, null).getEquipmentCompatibilityKind()).isNull();

        Item item = item(ItemCategory.WEAPON, EquipmentCompatibilityKind.WEAPON);
        item.update(
                ItemName.of("Item"),
                ItemCategory.WEAPON,
                ItemType.ETC,
                null,
                Rarity.COMMON,
                BaseAttrs.empty(),
                false,
                null,
                null
        );

        assertThat(item.getEquipmentCompatibilityKind()).isNull();
    }

    private Item item(
            ItemCategory category,
            EquipmentCompatibilityKind equipmentCompatibilityKind
    ) {
        return Item.create(
                ItemName.of("Item"),
                category,
                ItemType.ETC,
                equipmentCompatibilityKind,
                Rarity.COMMON,
                BaseAttrs.empty(),
                false,
                null,
                null
        );
    }

    private static Stream<Arguments> validCompatibilityPairs() {
        return Stream.of(
                Arguments.of(EquipmentCompatibilityKind.WEAPON, ItemCategory.WEAPON),
                Arguments.of(EquipmentCompatibilityKind.HEAD, ItemCategory.ARMOR),
                Arguments.of(EquipmentCompatibilityKind.CHEST, ItemCategory.ARMOR),
                Arguments.of(EquipmentCompatibilityKind.LEGS, ItemCategory.ARMOR),
                Arguments.of(EquipmentCompatibilityKind.HANDS, ItemCategory.ARMOR),
                Arguments.of(EquipmentCompatibilityKind.FEET, ItemCategory.ARMOR),
                Arguments.of(EquipmentCompatibilityKind.NECK, ItemCategory.ACCESSORY),
                Arguments.of(EquipmentCompatibilityKind.RING, ItemCategory.ACCESSORY),
                Arguments.of(EquipmentCompatibilityKind.TRINKET, ItemCategory.ACCESSORY)
        );
    }

    private static Stream<Arguments> invalidCompatibilityPairs() {
        return Stream.of(
                Arguments.of(EquipmentCompatibilityKind.WEAPON, ItemCategory.ARMOR),
                Arguments.of(EquipmentCompatibilityKind.HEAD, ItemCategory.ACCESSORY),
                Arguments.of(EquipmentCompatibilityKind.LEGS, ItemCategory.WEAPON),
                Arguments.of(EquipmentCompatibilityKind.NECK, ItemCategory.ARMOR)
        );
    }
}
