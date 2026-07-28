package online.lifeasgame.inventory.api;

import online.lifeasgame.inventory.api.admin.mapper.AdminItemWebMapper;
import online.lifeasgame.inventory.api.admin.request.AdminItemRequest;
import online.lifeasgame.inventory.api.player.mapper.ItemWebMapper;
import online.lifeasgame.inventory.api.player.response.ItemResponse;
import online.lifeasgame.inventory.application.command.ItemCommand;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.domain.BaseAttrs;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemCode;
import online.lifeasgame.inventory.domain.ItemName;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Item API contract")
class ItemApiContractTest {

    @Test
    @DisplayName("Admin legacy create request는 code 입력 없이 기존 command로 변환된다")
    void keepsAdminLegacyCreateContract() {
        AdminItemRequest.Create request = new AdminItemRequest.Create(
                "Legacy Item",
                "MISC",
                "ETC",
                "COMMON",
                Map.of(),
                false,
                null,
                null
        );

        ItemCommand.Create command = AdminItemWebMapper.toCreateCommand(request);

        assertThat(command.name()).isEqualTo("Legacy Item");
        assertThat(command.category()).isEqualTo("MISC");
        assertThat(command.type()).isEqualTo("ETC");
        assertThat(command.stackable()).isFalse();
    }

    @Test
    @DisplayName("Player content Item 응답은 stable code를 유실하지 않는다")
    void exposesContentCodeInPlayerResponse() {
        Item item = Item.createContentItem(
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

        ItemResponse.Detail response =
                ItemWebMapper.toDetail(ItemResult.Detail.from(item));

        assertThat(response.code()).isEqualTo("IT_FIRST_STEP_FRAGMENT");
        assertThat(response.maxDurability()).isNull();
    }
}
