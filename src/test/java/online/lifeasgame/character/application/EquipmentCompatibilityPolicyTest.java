package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Equipment compatibility policy")
class EquipmentCompatibilityPolicyTest {

    @ParameterizedTest
    @EnumSource(EquipmentSlotCategory.class)
    @DisplayName("explicit kind는 아홉 slot category의 exact match를 허용한다")
    void allowsExactExplicitKind(EquipmentSlotCategory slot) {
        assertThatCode(() -> validate(slot, "MISC", "ETC", slot.name()))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("explicitMismatches")
    @DisplayName("explicit kind가 slot category와 다르면 incompatible로 거부한다")
    void rejectsExplicitMismatch(
            EquipmentSlotCategory slot,
            String equipmentCompatibilityKind
    ) {
        assertError(
                slot,
                "MISC",
                "ETC",
                equipmentCompatibilityKind,
                PlayerEquipmentError.ITEM_NOT_COMPATIBLE_WITH_SLOT
        );
    }

    @ParameterizedTest(name = "{0} <- {1}/{2}")
    @MethodSource("legacyCompatiblePairs")
    @DisplayName("kind가 없으면 네 legacy-safe 조합을 허용한다")
    void allowsLegacySafePair(
            EquipmentSlotCategory slot,
            String itemCategory,
            String itemType
    ) {
        assertThatCode(() -> validate(slot, itemCategory, itemType, null))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(
            value = EquipmentSlotCategory.class,
            names = {"LEGS", "HANDS", "FEET", "NECK", "TRINKET"}
    )
    @DisplayName("kind가 없으면 다섯 legacy 미지원 slot을 unsupported로 거부한다")
    void rejectsLegacyUnsupportedSlot(EquipmentSlotCategory slot) {
        assertError(
                slot,
                "MISC",
                "ETC",
                null,
                PlayerEquipmentError.UNSUPPORTED_EQUIPMENT_SLOT
        );
    }

    private void validate(
            EquipmentSlotCategory slot,
            String category,
            String type,
            String equipmentCompatibilityKind
    ) {
        EquipmentCompatibilityPolicy.validate(
                slot,
                category,
                type,
                equipmentCompatibilityKind
        );
    }

    private void assertError(
            EquipmentSlotCategory slot,
            String category,
            String type,
            String equipmentCompatibilityKind,
            PlayerEquipmentError error
    ) {
        assertThatThrownBy(() -> validate(
                slot,
                category,
                type,
                equipmentCompatibilityKind
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(error)
        );
    }

    private static Stream<Arguments> explicitMismatches() {
        return Stream.of(
                Arguments.of(EquipmentSlotCategory.HEAD, "CHEST"),
                Arguments.of(EquipmentSlotCategory.LEGS, "HANDS")
        );
    }

    private static Stream<Arguments> legacyCompatiblePairs() {
        return Stream.of(
                Arguments.of(EquipmentSlotCategory.WEAPON, "WEAPON", "SWORD"),
                Arguments.of(EquipmentSlotCategory.HEAD, "ARMOR", "HELMET"),
                Arguments.of(EquipmentSlotCategory.CHEST, "ARMOR", "CHEST"),
                Arguments.of(EquipmentSlotCategory.RING, "ACCESSORY", "RING")
        );
    }
}
