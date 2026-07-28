package online.lifeasgame.inventory.domain.seed;

import online.lifeasgame.inventory.domain.BaseAttrs;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SeedLevel1Item catalog")
class SeedLevel1ItemTest {

    @Test
    @DisplayName("P0 Seed는 정확히 FIRST_STEP_FRAGMENT 한 건이다")
    void containsExactlyOneP0Seed() {
        assertThat(SeedLevel1Item.values())
                .containsExactly(SeedLevel1Item.FIRST_STEP_FRAGMENT);
        assertThat(SeedLevel1Item.definitions()).hasSize(1);
    }

    @Test
    @DisplayName("stable code는 중복 없이 공식 문자열을 보존한다")
    void hasUniqueStableCode() {
        var definitions = SeedLevel1Item.definitions();
        var uniqueCodes = new HashSet<>(
                definitions.stream().map(ItemSeedDefinition::code).toList()
        );

        assertThat(uniqueCodes).hasSameSizeAs(definitions);
        assertThat(ItemContentCode.IT_FIRST_STEP_FRAGMENT.value())
                .isEqualTo("IT_FIRST_STEP_FRAGMENT");
    }

    @Test
    @DisplayName("MEMORY_FRAGMENT metadata 대신 요청된 최소 Runtime 매핑만 정의한다")
    void mapsOfficialContentToCurrentRuntimeTaxonomy() {
        ItemSeedDefinition definition = SeedLevel1Item.FIRST_STEP_FRAGMENT.definition();

        assertThat(definition.code()).isEqualTo(ItemContentCode.IT_FIRST_STEP_FRAGMENT);
        assertThat(definition.name()).isEqualTo("첫걸음의 조각");
        assertThat(definition.category()).isEqualTo(ItemCategory.QUEST);
        assertThat(definition.type()).isEqualTo(ItemType.ETC);
        assertThat(definition.rarity()).isEqualTo(Rarity.COMMON);
        assertThat(definition.baseAttrs()).isEqualTo(BaseAttrs.empty());
        assertThat(definition.stackable()).isTrue();
        assertThat(definition.maxStack()).isEqualTo(99);
        assertThat(definition.maxDurability()).isNull();
    }

    @Test
    @DisplayName("catalog가 노출하는 collection과 baseAttrs는 변경할 수 없다")
    void exposesNoMutableState() {
        assertThatThrownBy(() -> SeedLevel1Item.definitions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> SeedLevel1Item.FIRST_STEP_FRAGMENT
                .definition()
                .baseAttrs()
                .attrs()
                .put("strength", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
