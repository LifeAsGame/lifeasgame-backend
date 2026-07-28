package online.lifeasgame.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Item content factory")
class ItemContentFactoryTest {

    @Test
    @DisplayName("legacy factory는 code가 없는 Item 생성을 유지한다")
    void createsLegacyItemWithoutCode() {
        Item item = Item.create(
                ItemName.of("Legacy Item"),
                ItemCategory.MISC,
                ItemType.ETC,
                Rarity.COMMON,
                BaseAttrs.empty(),
                false,
                null,
                null
        );

        assertThat(item.getCode()).isNull();
        assertThat(item.maxStack()).isEqualTo(1);
        assertThat(item.maxDurability()).isNull();
    }

    @Test
    @DisplayName("content factory는 stable code와 요청된 최소 Runtime 매핑으로 생성한다")
    void createsContentItemWithRuntimeMapping() {
        Item item = firstStepFragment();

        assertThat(item.getCode().value()).isEqualTo("IT_FIRST_STEP_FRAGMENT");
        assertThat(item.getName().value()).isEqualTo("첫걸음의 조각");
        assertThat(item.getCategory()).isEqualTo(ItemCategory.QUEST);
        assertThat(item.getType()).isEqualTo(ItemType.ETC);
        assertThat(item.getRarity()).isEqualTo(Rarity.COMMON);
        assertThat(item.getBaseAttrs()).isEqualTo(BaseAttrs.empty());
        assertThat(item.isStackable()).isTrue();
        assertThat(item.maxStack()).isEqualTo(99);
        assertThat(item.maxDurability()).isNull();
    }

    @Test
    @DisplayName("content factory는 null code를 거부한다")
    void rejectsContentItemWithoutCode() {
        assertThatThrownBy(() -> Item.createContentItem(
                null,
                ItemName.of("첫걸음의 조각"),
                ItemCategory.QUEST,
                ItemType.ETC,
                Rarity.COMMON,
                BaseAttrs.empty(),
                true,
                99,
                null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("일반 속성을 수정해도 content code는 바뀌지 않는다")
    void keepsContentCodeImmutableOnUpdate() {
        Item item = firstStepFragment();

        item.update(
                ItemName.of("첫걸음의 조각 수정"),
                ItemCategory.MISC,
                ItemType.ETC,
                Rarity.UNCOMMON,
                BaseAttrs.empty(),
                true,
                50,
                null
        );

        assertThat(item.getCode().value()).isEqualTo("IT_FIRST_STEP_FRAGMENT");
    }

    private Item firstStepFragment() {
        return Item.createContentItem(
                ItemCode.of("IT_FIRST_STEP_FRAGMENT"),
                ItemName.of("첫걸음의 조각"),
                ItemCategory.QUEST,
                ItemType.ETC,
                Rarity.COMMON,
                BaseAttrs.empty(),
                true,
                99,
                null
        );
    }
}
