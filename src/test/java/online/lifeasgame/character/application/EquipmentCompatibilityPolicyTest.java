package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Equipment compatibility policy")
class EquipmentCompatibilityPolicyTest {

    @Nested
    @DisplayName("검증된 슬롯과 item 조합이면")
    class SupportedPair {

        @Test
        @DisplayName("WEAPON, HEAD, CHEST, RING 조합만 허용한다")
        void allowsVerifiedPairs() {
            assertThatCode(() -> validate(
                    EquipmentSlotCategory.WEAPON,
                    "WEAPON",
                    "SWORD"
            )).doesNotThrowAnyException();
            assertThatCode(() -> validate(
                    EquipmentSlotCategory.HEAD,
                    "ARMOR",
                    "HELMET"
            )).doesNotThrowAnyException();
            assertThatCode(() -> validate(
                    EquipmentSlotCategory.CHEST,
                    "ARMOR",
                    "CHEST"
            )).doesNotThrowAnyException();
            assertThatCode(() -> validate(
                    EquipmentSlotCategory.RING,
                    "ACCESSORY",
                    "RING"
            )).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("검증된 슬롯에 다른 item 조합이면")
    class IncompatiblePair {

        @Test
        @DisplayName("category와 type을 정확히 비교해 incompatible로 거부한다")
        void rejectsIncompatiblePairs() {
            assertError(
                    EquipmentSlotCategory.WEAPON,
                    "ARMOR",
                    "HELMET",
                    PlayerEquipmentError.ITEM_NOT_COMPATIBLE_WITH_SLOT
            );
            assertError(
                    EquipmentSlotCategory.HEAD,
                    "ARMOR",
                    "CHEST",
                    PlayerEquipmentError.ITEM_NOT_COMPATIBLE_WITH_SLOT
            );
            assertError(
                    EquipmentSlotCategory.CHEST,
                    "ARMOR",
                    "HELMET",
                    PlayerEquipmentError.ITEM_NOT_COMPATIBLE_WITH_SLOT
            );
            assertError(
                    EquipmentSlotCategory.RING,
                    "ACCESSORY",
                    "ETC",
                    PlayerEquipmentError.ITEM_NOT_COMPATIBLE_WITH_SLOT
            );
        }
    }

    @Nested
    @DisplayName("아직 검증되지 않은 슬롯이면")
    class UnsupportedSlot {

        @Test
        @DisplayName("LEGS, HANDS, FEET, NECK, TRINKET을 distinct error로 거부한다")
        void rejectsUnsupportedSlots() {
            for (EquipmentSlotCategory slot : new EquipmentSlotCategory[]{
                    EquipmentSlotCategory.LEGS,
                    EquipmentSlotCategory.HANDS,
                    EquipmentSlotCategory.FEET,
                    EquipmentSlotCategory.NECK,
                    EquipmentSlotCategory.TRINKET
            }) {
                assertError(
                        slot,
                        "MISC",
                        "ETC",
                        PlayerEquipmentError.UNSUPPORTED_EQUIPMENT_SLOT
                );
            }
        }
    }

    private void validate(
            EquipmentSlotCategory slot,
            String category,
            String type
    ) {
        EquipmentCompatibilityPolicy.validate(slot, category, type);
    }

    private void assertError(
            EquipmentSlotCategory slot,
            String category,
            String type,
            PlayerEquipmentError error
    ) {
        assertThatThrownBy(() -> validate(slot, category, type))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
