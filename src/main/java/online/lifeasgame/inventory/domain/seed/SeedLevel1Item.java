package online.lifeasgame.inventory.domain.seed;

import online.lifeasgame.inventory.domain.BaseAttrs;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;

import java.util.Arrays;
import java.util.List;

public enum SeedLevel1Item {

    RECORD_CRYSTAL(new ItemSeedDefinition(
            ItemContentCode.IT_RECORD_CRYSTAL, "기록 결정", ItemCategory.MISC,
            ItemType.ETC, Rarity.COMMON, BaseAttrs.empty(), true, 99, null,
            "활동 기록 퀘스트에서 얻는 수집품. 보관하거나 거래할 수 있습니다.", false)),

    FIRST_STEP_FRAGMENT(
            new ItemSeedDefinition(
                    ItemContentCode.IT_FIRST_STEP_FRAGMENT,
                    "첫걸음의 조각",
                    ItemCategory.QUEST,
                    ItemType.ETC,
                    Rarity.COMMON,
                    BaseAttrs.empty(),
                    true,
                    99,
                    null,
                    null,
                    true
            )
    );

    private final ItemSeedDefinition definition;

    SeedLevel1Item(ItemSeedDefinition definition) {
        this.definition = definition;
    }

    public ItemSeedDefinition definition() {
        return definition;
    }

    public static List<ItemSeedDefinition> definitions() {
        return Arrays.stream(values())
                .map(SeedLevel1Item::definition)
                .toList();
    }
}
