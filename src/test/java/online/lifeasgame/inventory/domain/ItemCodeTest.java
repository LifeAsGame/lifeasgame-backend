package online.lifeasgame.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ItemCode")
class ItemCodeTest {

    @Test
    @DisplayName("앞뒤 공백만 제거하고 원래 대소문자를 보존한다")
    void trimsAndPreservesCase() {
        ItemCode code = ItemCode.of("  IT_First_Step_Fragment  ");

        assertThat(code.value()).isEqualTo("IT_First_Step_Fragment");
    }

    @Test
    @DisplayName("같은 문자열 값은 동등하다")
    void hasValueEquality() {
        assertThat(ItemCode.of("IT_FIRST_STEP_FRAGMENT"))
                .isEqualTo(ItemCode.of("IT_FIRST_STEP_FRAGMENT"))
                .hasSameHashCodeAs(ItemCode.of("IT_FIRST_STEP_FRAGMENT"));
    }

    @Test
    @DisplayName("null과 blank code를 거부한다")
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> ItemCode.of(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ItemCode.of(" \t "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("80자를 초과하는 code를 거부한다")
    void rejectsTooLongCode() {
        assertThatThrownBy(() -> ItemCode.of("I".repeat(ItemCode.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
