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
    @DisplayName("기념품과 거래용 수집품을 별도 콘텐츠로 유지한다")
    void containsSouvenirAndCollectible() {
        assertThat(SeedLevel1Item.values())
                .containsExactly(SeedLevel1Item.RECORD_CRYSTAL, SeedLevel1Item.FIRST_STEP_FRAGMENT);
        assertThat(SeedLevel1Item.definitions()).hasSize(2);
        var crystal = SeedLevel1Item.RECORD_CRYSTAL.definition();
        assertThat(crystal.rewardBound()).isFalse();
        assertThat(crystal.description()).isEqualTo("활동 기록 퀘스트에서 얻는 수집품. 보관하거나 거래할 수 있습니다.");
        assertThat(crystal.category()).isEqualTo(ItemCategory.MISC);
        assertThat(crystal.type()).isEqualTo(ItemType.ETC);
        assertThat(crystal.stackable()).isTrue();
        assertThat(crystal.maxStack()).isEqualTo(99);
        assertThat(SeedLevel1Item.FIRST_STEP_FRAGMENT.definition().rewardBound()).isTrue();
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
