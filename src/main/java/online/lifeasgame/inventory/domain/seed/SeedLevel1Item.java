package online.lifeasgame.inventory.domain.seed;

import online.lifeasgame.inventory.domain.BaseAttrs;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;

import java.util.Arrays;
import java.util.List;

public enum SeedLevel1Item {

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
                    null
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
