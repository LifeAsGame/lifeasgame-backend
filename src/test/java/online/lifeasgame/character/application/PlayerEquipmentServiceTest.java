package online.lifeasgame.character.application;

import online.lifeasgame.character.application.command.PlayerEquipmentCommand;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentAvailabilityApi;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentReadApi;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Player equipment write")
class PlayerEquipmentServiceTest {

    private static final Long PLAYER_ID = 262L;
    private static final Long SLOT_ID = 21L;
    private static final Long ITEM_INSTANCE_ID = 31L;
    private static final Long PREVIOUS_ITEM_INSTANCE_ID = 30L;

    @Mock
    private PlayerEquipmentWriter writer;

    @Mock
    private PlayerEquipmentReader reader;

    @Mock
    private EquipmentSlotReader slotReader;

    @Mock
    private InventoryEquipmentReadApi inventoryEquipmentReadApi;

    @Mock
    private InventoryEquipmentAvailabilityApi inventoryEquipmentAvailabilityApi;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    private PlayerEquipmentService service;

    @BeforeEach
    void setUp() {
        service = new PlayerEquipmentService(
                writer,
                reader,
                slotReader,
                inventoryEquipmentReadApi,
                inventoryEquipmentAvailabilityApi,
                currentPlayerAccessor
        );
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willReturn(PLAYER_ID);
    }

    @Nested
    @DisplayName("호환성이 승인되지 않은 authority slot에 장착할 때")
    class EquipInapplicableSlot {

        @Test
        @DisplayName("Inventory를 조회하기 전에 unsupported로 거부한다")
        void rejectsWithoutActivatingCompatibility() {
            EquipmentSlot slot = mock(EquipmentSlot.class);
            given(slotReader.getByIdOrThrow(SLOT_ID)).willReturn(slot);

            assertThatThrownBy(() -> service.equip(command(
                    SLOT_ID,
                    ITEM_INSTANCE_ID
            ))).isInstanceOfSatisfying(DomainException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(
                            PlayerEquipmentError.UNSUPPORTED_EQUIPMENT_SLOT
                    )
            );
            verifyNoInteractions(
                    inventoryEquipmentReadApi,
                    reader,
                    writer,
                    inventoryEquipmentAvailabilityApi
            );
        }
    }

    @Nested
    @DisplayName("소유한 item을 장착할 때")
    class EquipOwnedItem {

        @Test
        @DisplayName("slot, ownership, compatibility, uniqueness 검증 후 target row를 갱신한다")
        void equipsVerifiedOwnedItemInOrder() {
            EquipmentSlot slot = slot(EquipmentSlotCategory.WEAPON);
            InventoryEquipmentReadApi.OwnedEquipmentItem item = item(
                    ITEM_INSTANCE_ID,
                    "WEAPON",
                    "SWORD"
            );
            PlayerEquipment equipment = PlayerEquipment.create(
                    PLAYER_ID,
                    SLOT_ID,
                    ITEM_INSTANCE_ID
            );
            given(slotReader.getByIdOrThrow(SLOT_ID)).willReturn(slot);
            given(inventoryEquipmentReadApi.getOwnedItem(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            )).willReturn(item);
            given(writer.equip(PLAYER_ID, SLOT_ID, ITEM_INSTANCE_ID))
                    .willReturn(replacement(
                            equipment,
                            PREVIOUS_ITEM_INSTANCE_ID
                    ));

            var result = service.equip(command(SLOT_ID, ITEM_INSTANCE_ID));

            assertThat(result.slotId()).isEqualTo(SLOT_ID);
            assertThat(result.itemInstanceId()).isEqualTo(ITEM_INSTANCE_ID);
            InOrder order = inOrder(
                    currentPlayerAccessor,
                    slotReader,
                    inventoryEquipmentReadApi,
                    reader,
                    writer,
                    inventoryEquipmentAvailabilityApi
            );
            order.verify(currentPlayerAccessor).currentPlayerIdOrThrow();
            order.verify(slotReader).getByIdOrThrow(SLOT_ID);
            order.verify(inventoryEquipmentReadApi).getOwnedItem(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            );
            order.verify(reader).assertNotEquipped(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            );
            order.verify(writer).equip(
                    PLAYER_ID,
                    SLOT_ID,
                    ITEM_INSTANCE_ID
            );
            order.verify(inventoryEquipmentAvailabilityApi)
                    .replaceEquippedItem(
                            PLAYER_ID,
                            PREVIOUS_ITEM_INSTANCE_ID,
                            ITEM_INSTANCE_ID
                    );
        }

        @Test
        @DisplayName("서로 다른 item은 서로 다른 슬롯에 각각 장착할 수 있다")
        void equipsDifferentItemsInDifferentSlots() {
            Long secondSlotId = 22L;
            Long secondItemId = 32L;
            given(slotReader.getByIdOrThrow(SLOT_ID))
                    .willReturn(slot(EquipmentSlotCategory.WEAPON));
            given(slotReader.getByIdOrThrow(secondSlotId))
                    .willReturn(slot(EquipmentSlotCategory.HEAD));
            given(inventoryEquipmentReadApi.getOwnedItem(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            )).willReturn(item(ITEM_INSTANCE_ID, "WEAPON", "SWORD"));
            given(inventoryEquipmentReadApi.getOwnedItem(
                    PLAYER_ID,
                    secondItemId
            )).willReturn(item(secondItemId, "ARMOR", "HELMET"));
            given(writer.equip(PLAYER_ID, SLOT_ID, ITEM_INSTANCE_ID))
                    .willReturn(replacement(PlayerEquipment.create(
                            PLAYER_ID,
                            SLOT_ID,
                            ITEM_INSTANCE_ID
                    ), null));
            given(writer.equip(PLAYER_ID, secondSlotId, secondItemId))
                    .willReturn(replacement(PlayerEquipment.create(
                            PLAYER_ID,
                            secondSlotId,
                            secondItemId
                    ), null));

            service.equip(command(SLOT_ID, ITEM_INSTANCE_ID));
            service.equip(command(secondSlotId, secondItemId));

            verify(writer).equip(PLAYER_ID, SLOT_ID, ITEM_INSTANCE_ID);
            verify(writer).equip(PLAYER_ID, secondSlotId, secondItemId);
            verify(inventoryEquipmentAvailabilityApi).replaceEquippedItem(
                    PLAYER_ID,
                    null,
                    ITEM_INSTANCE_ID
            );
            verify(inventoryEquipmentAvailabilityApi).replaceEquippedItem(
                    PLAYER_ID,
                    null,
                    secondItemId
            );
        }
    }

    @Nested
    @DisplayName("소유하지 않은 item을 장착할 때")
    class EquipUnavailableItem {

        @Test
        @DisplayName("foreign과 nonexistent instance를 같은 Inventory not-found로 거부한다")
        void rejectsForeignAndMissingItemsWithoutDisclosure() {
            given(slotReader.getByIdOrThrow(SLOT_ID))
                    .willReturn(slot(EquipmentSlotCategory.WEAPON));
            given(inventoryEquipmentReadApi.getOwnedItem(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            )).willThrow(new DomainException(
                    InventoryError.INVENTORY_ENTRY_NOT_FOUND
            ));

            assertThatThrownBy(() -> service.equip(command(
                    SLOT_ID,
                    ITEM_INSTANCE_ID
            ))).isInstanceOfSatisfying(DomainException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(
                            InventoryError.INVENTORY_ENTRY_NOT_FOUND
                    )
            );
            verify(reader, never()).assertNotEquipped(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            );
            verifyNoInteractions(writer, inventoryEquipmentAvailabilityApi);
        }
    }

    @Nested
    @DisplayName("이미 다른 슬롯에 장착한 item이면")
    class EquipDuplicateItem {

        @Test
        @DisplayName("player-wide conflict로 거부하고 implicit move하지 않는다")
        void rejectsAlreadyEquippedItemAcrossSlots() {
            given(slotReader.getByIdOrThrow(SLOT_ID))
                    .willReturn(slot(EquipmentSlotCategory.RING));
            given(inventoryEquipmentReadApi.getOwnedItem(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            )).willReturn(item(
                    ITEM_INSTANCE_ID,
                    "ACCESSORY",
                    "RING"
            ));
            willThrow(new DomainException(
                    PlayerEquipmentError.ALREADY_EQUIPPED_ITEM
            )).given(reader).assertNotEquipped(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            );

            assertThatThrownBy(() -> service.equip(command(
                    SLOT_ID,
                    ITEM_INSTANCE_ID
            ))).isInstanceOfSatisfying(DomainException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(
                            PlayerEquipmentError.ALREADY_EQUIPPED_ITEM
                    )
            );
            verifyNoInteractions(writer, inventoryEquipmentAvailabilityApi);
        }
    }

    @Nested
    @DisplayName("장착을 해제할 때")
    class UnequipItem {

        @Test
        @DisplayName("equipment row 해제 후 Inventory availability를 FREE로 돌린다")
        void releasesInventoryAvailability() {
            given(writer.unEquip(PLAYER_ID, SLOT_ID))
                    .willReturn(ITEM_INSTANCE_ID);

            service.unEquip(SLOT_ID);

            verify(writer).unEquip(PLAYER_ID, SLOT_ID);
            verify(inventoryEquipmentAvailabilityApi).releaseEquippedItem(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            );
            verifyNoInteractions(slotReader, inventoryEquipmentReadApi, reader);
        }
    }

    private PlayerEquipmentCommand.Equip command(
            Long slotId,
            Long itemInstanceId
    ) {
        return new PlayerEquipmentCommand.Equip(slotId, itemInstanceId);
    }

    private EquipmentSlot slot(EquipmentSlotCategory category) {
        return EquipmentSlot.of("SLOT_" + category, "slot", category);
    }

    private InventoryEquipmentReadApi.OwnedEquipmentItem item(
            Long itemInstanceId,
            String category,
            String type
    ) {
        return new InventoryEquipmentReadApi.OwnedEquipmentItem(
                itemInstanceId,
                itemInstanceId + 1_000,
                category,
                type,
                null
        );
    }

    private PlayerEquipmentWriter.EquipmentReplacement replacement(
            PlayerEquipment equipment,
            Long previousItemInstanceId
    ) {
        return new PlayerEquipmentWriter.EquipmentReplacement(
                equipment,
                previousItemInstanceId
        );
    }
}
